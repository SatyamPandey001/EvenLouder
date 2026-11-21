package com.evenlouder.app.core.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.dsp.DynamicsEngine
import com.evenlouder.app.core.dsp.TuningProfile
import com.evenlouder.app.ui.MainActivity
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * Foreground Service maintaining hardware-safe DynamicsProcessing sessions.
 * Features universal active session discovery across browsers, video players, and music apps,
 * clean physical bypass, and silent notifications.
 */
class AudioEngineService : Service() {

    companion object {
        private const val TAG = "AudioEngineService"
        private const val CHANNEL_ID = "evenlouder_audio_channel_v3"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_ATTACH_SESSION = "com.evenlouder.ACTION_ATTACH_SESSION"
        const val ACTION_DETACH_SESSION = "com.evenlouder.ACTION_DETACH_SESSION"
        const val ACTION_TOGGLE_DSP = "com.evenlouder.ACTION_TOGGLE_DSP"
        const val ACTION_SET_PROFILE = "com.evenlouder.ACTION_SET_PROFILE"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_CLIENT_PKG = "extra_client_pkg"
        const val EXTRA_PROFILE_NAME = "extra_profile_name"

        // AudioService PlaybackConfiguration regex (for standard media session dumps)
        val DUMPSYS_CONFIG_REGEX = Regex("""u/pid:(\d+)/\d+.*?state:(started|paused)\b.*?sessionId:(\d+)""")
        // AudioService player event regex (package tag is optional so non-media apps are not missed)
        val DUMPSYS_EVENT_REGEX = Regex("""uid/pid:(\d+)/\d+(?:\s+package:([^\s]+))?.*?session:(\d+)""")

        // AudioFlinger Global session refs table (e.g. "7201 1 15599 10218 com.brave.browser")
        val AUDIOFLINGER_GLOBAL_REF_REGEX = Regex("""^\s*(\d+)\s+\d+\s+\d+\s+(\d+)\s+([a-zA-Z0-9_.]+)""")
        // AudioFlinger active tracks table row (e.g. "896 yes 15599/ 10218 7201")
        val AUDIOFLINGER_TRACK_REGEX = Regex("""\b(?:yes|ACTIVE)\s+(?:(?:\d+/\s*)?(\d+))\s+(\d+)""")
        val AUDIOFLINGER_ACTIVE_TRACK_REGEX = Regex("""^\s*\S+\s+(\d+)\s+(\d+)\s+ACTIVE\b""")
        val AUDIOFLINGER_ACTIVE_REGEX = Regex("""Client:\s*(\d+).*?Session:\s*(\d+)""", RegexOption.IGNORE_CASE)

        @Volatile
        var instance: AudioEngineService? = null
            private set
    }

    enum class SessionOrigin {
        INTERNAL,
        BROADCAST,
        AUTO_SNIFFER
    }

    inner class LocalBinder : Binder() {
        val service: AudioEngineService get() = this@AudioEngineService
    }

    private val binder = LocalBinder()
    private val activeEngines = ConcurrentHashMap<Int, DynamicsEngine>()
    private val sessionPackages = ConcurrentHashMap<Int, String>()
    private val sessionOrigins = ConcurrentHashMap<Int, SessionOrigin>()
    private val missedScanCounts = ConcurrentHashMap<Int, Int>()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scannerJob: Job? = null
    private var playbackCallback: AudioManager.AudioPlaybackCallback? = null

    var currentConfig: DspConfig = TuningProfile.DOLBY_SPATIAL.config
        private set

    var currentProfileName: String = TuningProfile.DOLBY_SPATIAL.title
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerPlaybackCallback()
        if (currentConfig.globalHookEnabled) {
            startSessionScanner()
        }
        Log.i(TAG, "AudioEngineService started in foreground")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_ATTACH_SESSION -> {
                    val sessionId = it.getIntExtra(EXTRA_SESSION_ID, 0)
                    val pkg = it.getStringExtra(EXTRA_CLIENT_PKG) ?: "External"
                    if (sessionId > 0) attachSession(sessionId, pkg)
                }
                ACTION_DETACH_SESSION -> {
                    val sessionId = it.getIntExtra(EXTRA_SESSION_ID, 0)
                    if (sessionId > 0) detachSession(sessionId)
                }
                ACTION_TOGGLE_DSP -> {
                    updateConfig(currentConfig.copy(enabled = !currentConfig.enabled))
                }
                ACTION_SET_PROFILE -> {
                    val profileName = it.getStringExtra(EXTRA_PROFILE_NAME)
                    val profile = TuningProfile.values().firstOrNull { p -> p.name == profileName }
                    profile?.let { p ->
                        currentProfileName = p.title
                        updateConfig(p.config.copy(globalHookEnabled = currentConfig.globalHookEnabled), p.title)
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    @Synchronized
    fun attachSession(sessionId: Int, pkg: String = "Internal", origin: SessionOrigin = SessionOrigin.INTERNAL) {
        if (activeEngines.containsKey(sessionId)) {
            if (origin == SessionOrigin.BROADCAST) {
                sessionOrigins[sessionId] = SessionOrigin.BROADCAST
                sessionPackages[sessionId] = pkg
            }
            return
        }

        try {
            val engine = DynamicsEngine(audioSessionId = sessionId, channelCount = 2)
            engine.applyConfig(currentConfig)
            activeEngines[sessionId] = engine
            sessionPackages[sessionId] = pkg
            sessionOrigins[sessionId] = origin
            updateNotification()
            Log.i(TAG, "Attached DSP to session $sessionId ($pkg, origin: $origin). Total active: ${activeEngines.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed attaching session $sessionId", e)
        }
    }

    @Synchronized
    fun detachSession(sessionId: Int) {
        missedScanCounts.remove(sessionId)
        activeEngines.remove(sessionId)?.let { engine ->
            engine.release()
            sessionPackages.remove(sessionId)
            sessionOrigins.remove(sessionId)
            updateNotification()
            Log.i(TAG, "Detached DSP from session $sessionId. Remaining: ${activeEngines.size}")
        }
    }

    fun hasDumpPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.DUMP) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    @Synchronized
    fun updateConfig(newConfig: DspConfig, profileTitle: String? = null) {
        val wasGlobal = currentConfig.globalHookEnabled
        val wasEnabled = currentConfig.enabled
        currentConfig = newConfig
        if (profileTitle != null) {
            currentProfileName = profileTitle
        }

        // Apply updated config (or physical bypass) to all tracked sessions
        for ((_, engine) in activeEngines) {
            engine.applyConfig(newConfig)
        }

        if (!newConfig.enabled && wasEnabled) {
            Log.i(TAG, "DSP TURNED OFF: Bypassed all active effects on hardware channels")
        } else if (newConfig.enabled && !wasEnabled) {
            Log.i(TAG, "DSP TURNED ON: Re-enabled all tracked sessions and scanning for new ones")
            if (newConfig.globalHookEnabled) {
                serviceScope.launch { scanActiveSessions() }
            }
        }

        if (newConfig.globalHookEnabled && !wasGlobal) {
            startSessionScanner()
        } else if (!newConfig.globalHookEnabled && wasGlobal) {
            stopSessionScanner()
            val external = activeEngines.keys.filter { id ->
                sessionOrigins[id] != SessionOrigin.INTERNAL
            }
            for (id in external) {
                detachSession(id)
            }
        }

        updateNotification()
    }

    fun getActiveSessions(): List<Pair<Int, String>> {
        return activeEngines.keys.map { id -> id to (sessionPackages[id] ?: "Active Stream") }
    }

    private fun registerPlaybackCallback() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            playbackCallback = object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                    if (currentConfig.globalHookEnabled) {
                        serviceScope.launch { scanActiveSessions() }
                    }
                }
            }
            playbackCallback?.let { audioManager.registerAudioPlaybackCallback(it, null) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed registering AudioPlaybackCallback", e)
        }
    }

    private fun startSessionScanner() {
        scannerJob?.cancel()
        if (!hasDumpPermission()) {
            Log.i(TAG, "DUMP permission not granted; passive broadcast mode active (zero polling overhead)")
            return
        }
        scannerJob = serviceScope.launch {
            while (isActive && currentConfig.globalHookEnabled) {
                scanActiveSessions()
                // shortcut: 15s poll saves CPU/battery; AudioPlaybackCallback handles instant stream start/stop events
                delay(15000)
            }
        }
        Log.i(TAG, "Started background audio session scanner (audioflinger & audio)")
    }

    private fun stopSessionScanner() {
        scannerJob?.cancel()
        scannerJob = null
        Log.i(TAG, "Stopped background audio session scanner")
    }

    private fun isValidExternalSession(uid: Int, sessionId: Int, pkg: String? = null): Boolean {
        if (uid == android.os.Process.myUid() || uid == 1000 || uid == 1001 || uid == 1013 || uid == 1041 || uid <= 0) return false
        if (pkg != null && (pkg.contains("systemui") || pkg.contains("evenlouder"))) return false
        return sessionId > 0
    }

    private fun resolvePackageName(uid: Int): String {
        return packageManager.getPackagesForUid(uid)?.firstOrNull() ?: "App (UID $uid)"
    }

    private fun parseAudioDump(command: String, discovered: MutableMap<Int, String>) {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.useLines { lines ->
                for (line in lines) {
                    // 1. AudioFlinger Global session refs table (e.g. "7201 1 15599 10218 com.brave.browser")
                    val globalRefMatch = AUDIOFLINGER_GLOBAL_REF_REGEX.find(line)
                    if (globalRefMatch != null) {
                        val sessionId = globalRefMatch.groupValues[1].toIntOrNull() ?: continue
                        val uid = globalRefMatch.groupValues[2].toIntOrNull() ?: continue
                        val pkg = globalRefMatch.groupValues[3]
                        if (isValidExternalSession(uid, sessionId, pkg)) {
                            discovered[sessionId] = pkg
                        }
                        continue
                    }

                    // 2. AudioFlinger active tracks table row (e.g. "896 yes 15599/ 10218 7201 ...")
                    val flingerTrackMatch = AUDIOFLINGER_TRACK_REGEX.find(line)
                    if (flingerTrackMatch != null) {
                        val uid = flingerTrackMatch.groupValues[1].toIntOrNull() ?: continue
                        val sessionId = flingerTrackMatch.groupValues[2].toIntOrNull() ?: continue
                        val pkg = resolvePackageName(uid)
                        if (isValidExternalSession(uid, sessionId, pkg)) {
                            discovered[sessionId] = pkg
                        }
                        continue
                    }

                    // 3. AudioFlinger alternative track format (e.g. "1 10080 7721 ACTIVE")
                    val altTrackMatch = AUDIOFLINGER_ACTIVE_TRACK_REGEX.find(line)
                    if (altTrackMatch != null) {
                        val uid = altTrackMatch.groupValues[1].toIntOrNull() ?: continue
                        val sessionId = altTrackMatch.groupValues[2].toIntOrNull() ?: continue
                        val pkg = resolvePackageName(uid)
                        if (isValidExternalSession(uid, sessionId, pkg)) {
                            discovered[sessionId] = pkg
                        }
                        continue
                    }

                    // 4. AudioFlinger active tracks block (e.g. "Client: 10228, Session: 6601")
                    val flingerActiveMatch = AUDIOFLINGER_ACTIVE_REGEX.find(line)
                    if (flingerActiveMatch != null) {
                        val uid = flingerActiveMatch.groupValues[1].toIntOrNull() ?: continue
                        val sessionId = flingerActiveMatch.groupValues[2].toIntOrNull() ?: continue
                        val pkg = resolvePackageName(uid)
                        if (isValidExternalSession(uid, sessionId, pkg)) {
                            discovered[sessionId] = pkg
                        }
                        continue
                    }

                    // 5. AudioService PlaybackConfiguration line
                    val configMatch = DUMPSYS_CONFIG_REGEX.find(line)
                    if (configMatch != null) {
                        val uid = configMatch.groupValues[1].toIntOrNull() ?: continue
                        val state = configMatch.groupValues[2]
                        val sessionId = configMatch.groupValues[3].toIntOrNull() ?: continue
                        val pkg = resolvePackageName(uid)
                        if (isValidExternalSession(uid, sessionId, pkg) && state == "started") {
                            discovered[sessionId] = pkg
                        }
                        continue
                    }

                    // 6. AudioService player event line (package is optional)
                    val eventMatch = DUMPSYS_EVENT_REGEX.find(line)
                    if (eventMatch != null) {
                        val uid = eventMatch.groupValues[1].toIntOrNull() ?: continue
                        val explicitPkg = eventMatch.groupValues[2].takeIf { it.isNotBlank() }
                        val sessionId = eventMatch.groupValues[3].toIntOrNull() ?: continue
                        val pkg = explicitPkg ?: resolvePackageName(uid)
                        if (isValidExternalSession(uid, sessionId, pkg)) {
                            discovered[sessionId] = pkg
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing $command", e)
        } finally {
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
    }

    private fun scanActiveSessions() {
        if (!currentConfig.globalHookEnabled) return

        val discovered = mutableMapOf<Int, String>()

        if (hasDumpPermission()) {
            parseAudioDump("dumpsys media.audio_flinger", discovered)
            parseAudioDump("dumpsys audio", discovered)
        }

        // Attach newly discovered active sessions
        for ((sessionId, pkg) in discovered) {
            missedScanCounts[sessionId] = 0
            if (!activeEngines.containsKey(sessionId)) {
                attachSession(sessionId, pkg, SessionOrigin.AUTO_SNIFFER)
            }
        }

        // Detach sessions only after 2 consecutive missed scans to avoid transient audio drops
        if (discovered.isNotEmpty()) {
            val autoSniffed = activeEngines.keys.filter { id ->
                sessionOrigins[id] == SessionOrigin.AUTO_SNIFFER
            }
            for (id in autoSniffed) {
                if (!discovered.containsKey(id)) {
                    val missed = (missedScanCounts[id] ?: 0) + 1
                    missedScanCounts[id] = missed
                    if (missed >= 2) {
                        missedScanCounts.remove(id)
                        detachSession(id)
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "EvenLouder Audio Tuning Service",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Maintains micro-speaker dynamics tuning in the background"
            setShowBadge(false)
            setSound(null, null)      // Completely silent: no chimes or beeps on updates
            enableVibration(false)    // No vibration
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createActionPendingIntent(action: String, profileName: String? = null, reqCode: Int): PendingIntent {
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = action
            if (profileName != null) {
                putExtra(EXTRA_PROFILE_NAME, profileName)
            }
        }
        return PendingIntent.getBroadcast(
            this,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(): Notification {
        val count = activeEngines.size
        val isEnabled = currentConfig.enabled

        val toggleLabel = if (isEnabled) "Turn OFF" else "Turn ON"
        val togglePendingIntent = createActionPendingIntent(ACTION_TOGGLE_DSP, reqCode = 1)

        val dolbyPendingIntent = createActionPendingIntent(ACTION_SET_PROFILE, TuningProfile.DOLBY_SPATIAL.name, reqCode = 2)
        val maxxBassPendingIntent = createActionPendingIntent(ACTION_SET_PROFILE, TuningProfile.MAXX_BASS.name, reqCode = 3)
        val hifiPendingIntent = createActionPendingIntent(ACTION_SET_PROFILE, TuningProfile.DIRAC_HIFI.name, reqCode = 4)

        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryText = if (isEnabled) {
            "DSP ACTIVE • Profile: $currentProfileName ($count stream${if (count == 1) "" else "s"})"
        } else {
            "DSP BYPASSED • Pure Stock Speaker Audio"
        }

        val expandedDetails = if (isEnabled) {
            "Current Profile: $currentProfileName\n" +
                    "Active Streams: $count\n" +
                    "Loudness: +${"%.1f".format(currentConfig.loudnessGainDb)} dB • Vocal: +${"%.1f".format(currentConfig.vocalClarityDb)} dB • De-Box: ${"%.1f".format(currentConfig.boxinessCutDb)} dB • Bass: +${"%.1f".format(currentConfig.bassGainDb)} dB\n" +
                    "Limiter locked @ -0.5 dBFS"
        } else {
            "DSP is completely released from audio hardware. Tap Turn ON or select a profile to re-engage."
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("EvenLouder Audio Engine")
            .setContentText(summaryText)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Never beep or play sounds when updating state
            .setStyle(Notification.BigTextStyle().bigText(expandedDetails))
            .addAction(Notification.Action.Builder(null, toggleLabel, togglePendingIntent).build())
            .addAction(Notification.Action.Builder(null, "Cinema 3D", dolbyPendingIntent).build())
            .addAction(Notification.Action.Builder(null, "Club Bass", maxxBassPendingIntent).build())
            .addAction(Notification.Action.Builder(null, "Hi-Fi", hifiPendingIntent).build())
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSessionScanner()
        try {
            playbackCallback?.let {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.unregisterAudioPlaybackCallback(it)
            }
        } catch (_: Exception) {}

        serviceScope.cancel()
        for ((_, engine) in activeEngines) {
            engine.release()
        }
        activeEngines.clear()
        sessionPackages.clear()
        sessionOrigins.clear()
        missedScanCounts.clear()
        instance = null
        Log.i(TAG, "AudioEngineService destroyed")
    }
}

/**
 * Handles action clicks directly from notifications via Broadcasts.
 * Broadcasts execute immediately and bypass Android 14/15 background service launch restrictions.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = AudioEngineService.instance
        if (service == null) {
            val serviceIntent = Intent(context, AudioEngineService::class.java).apply {
                action = intent.action
                putExtras(intent)
            }
            context.startForegroundService(serviceIntent)
            return
        }

        when (intent.action) {
            AudioEngineService.ACTION_TOGGLE_DSP -> {
                service.updateConfig(service.currentConfig.copy(enabled = !service.currentConfig.enabled))
            }
            AudioEngineService.ACTION_SET_PROFILE -> {
                val profileName = intent.getStringExtra(AudioEngineService.EXTRA_PROFILE_NAME)
                val profile = TuningProfile.values().firstOrNull { it.name == profileName }
                profile?.let { p ->
                    service.updateConfig(p.config.copy(globalHookEnabled = service.currentConfig.globalHookEnabled), p.title)
                }
            }
        }
    }
}
