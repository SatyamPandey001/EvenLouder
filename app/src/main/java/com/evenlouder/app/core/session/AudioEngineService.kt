package com.evenlouder.app.core.session

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.SystemClock
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.evenlouder.app.R
import com.evenlouder.app.core.dsp.AcousticProfileRepository
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.dsp.DynamicsEngine
import com.evenlouder.app.core.dsp.TuningProfile
import com.evenlouder.app.core.dsp.applyingProfile
import com.evenlouder.app.ui.MainActivity
import com.evenlouder.app.widget.EvenLouderWidget
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
        private const val CHANNEL_ID = "evenlouder_audio_channel_v4"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_EXTERNAL_SESSIONS = 16     // caps resource use if apps spam broadcasts
        private const val FAILED_RETRY_MS = 2_000L        // fast retry for tracks still negotiating mixer buffers

        const val ACTION_ATTACH_SESSION = "com.evenlouder.ACTION_ATTACH_SESSION"
        const val ACTION_DETACH_SESSION = "com.evenlouder.ACTION_DETACH_SESSION"
        const val ACTION_TOGGLE_DSP = "com.evenlouder.ACTION_TOGGLE_DSP"
        const val ACTION_SET_PROFILE = "com.evenlouder.ACTION_SET_PROFILE"
        const val ACTION_NEXT_PROFILE = "com.evenlouder.ACTION_NEXT_PROFILE"
        const val ACTION_PREV_PROFILE = "com.evenlouder.ACTION_PREV_PROFILE"

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
    private val failedAttempts = ConcurrentHashMap<Int, Long>()
    private val failedAttemptCounts = ConcurrentHashMap<Int, Int>()
    private val applyChannel = Channel<Unit>(Channel.CONFLATED)

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scannerJob: Job? = null
    private var playbackCallback: AudioManager.AudioPlaybackCallback? = null

    @Volatile
    var currentConfig: DspConfig = TuningProfile.PHONE_SPEAKER.config
        private set

    var currentProfileName: String = TuningProfile.PHONE_SPEAKER.title
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        loadSavedConfig()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 34) {
            // Must match android:foregroundServiceType in the manifest
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        registerPlaybackCallback()
        startApplyWorker()
        if (currentConfig.globalHookEnabled) {
            startSessionScanner()
        }
        Log.i(TAG, "AudioEngineService started in foreground")
    }

    /** One serial worker: always applies the LATEST config, never overlaps, never runs on the UI thread. */
    private fun startApplyWorker() {
        serviceScope.launch {
            for (dummy in applyChannel) {
                val cfg = currentConfig
                for ((id, engine) in activeEngines) {
                    if (!engine.applyConfig(cfg)) {
                        Log.w(TAG, "Session $id unrecoverable, detaching")
                        detachSession(id)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_ATTACH_SESSION, AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                    val sessionId = it.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0).takeIf { id -> id > 0 }
                        ?: it.getIntExtra(EXTRA_SESSION_ID, 0)
                    val pkg = it.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
                        ?: it.getStringExtra(EXTRA_CLIENT_PKG)
                        ?: "External"
                    if (sessionId > 0 && currentConfig.globalHookEnabled) attachSession(sessionId, pkg, SessionOrigin.BROADCAST)
                }
                ACTION_DETACH_SESSION, AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                    val sessionId = it.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0).takeIf { id -> id > 0 }
                        ?: it.getIntExtra(EXTRA_SESSION_ID, 0)
                    if (sessionId > 0) detachSession(sessionId)
                }
                else -> handleControlAction(it.action, it.getStringExtra(EXTRA_PROFILE_NAME))
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

        if (origin != SessionOrigin.INTERNAL && activeEngines.size >= MAX_EXTERNAL_SESSIONS) {
            Log.w(TAG, "Session limit reached; ignoring session $sessionId")
            return
        }
        val now = SystemClock.elapsedRealtime()
        val fails = failedAttemptCounts[sessionId] ?: 0
        if (fails >= 3) {
            val lastFail = failedAttempts[sessionId]
            if (lastFail != null && now - lastFail < FAILED_RETRY_MS) return
        }

        try {
            var engine = DynamicsEngine(audioSessionId = sessionId, channelCount = 2)
            if (!engine.isReady) {
                engine.release()
                // Retry with mono channel (1 channel) for podcasts, audiobooks, and browser voice
                engine = DynamicsEngine(audioSessionId = sessionId, channelCount = 1)
            }
            if (!engine.isReady) {
                if (failedAttempts.size > 64) {
                    failedAttempts.clear()
                    failedAttemptCounts.clear()
                }
                failedAttemptCounts[sessionId] = fails + 1
                failedAttempts[sessionId] = now
                engine.release()
                Log.w(TAG, "Effect unavailable for session $sessionId ($pkg); will retry later")
                return
            }
            if (!engine.applyConfig(currentConfig)) {
                failedAttemptCounts[sessionId] = fails + 1
                failedAttempts[sessionId] = now
                engine.release()
                return
            }
            failedAttempts.remove(sessionId)
            failedAttemptCounts.remove(sessionId)
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
        failedAttempts.remove(sessionId)
        failedAttemptCounts.remove(sessionId)
        activeEngines.remove(sessionId)?.let { engine ->
            engine.release()
            sessionPackages.remove(sessionId)
            sessionOrigins.remove(sessionId)
            updateNotification()
            Log.i(TAG, "Detached DSP from session $sessionId. Remaining: ${activeEngines.size}")
        }
    }

    /** Single entry point for notification buttons, widget buttons and the quick settings tile. */
    fun handleControlAction(action: String?, profileName: String?) {
        when (action) {
            ACTION_TOGGLE_DSP -> toggleDsp()
            ACTION_NEXT_PROFILE -> cycleProfile(+1)
            ACTION_PREV_PROFILE -> cycleProfile(-1)
            ACTION_SET_PROFILE -> {
                val profile = TuningProfile.values().firstOrNull { p -> p.name == profileName } ?: return
                updateConfig(currentConfig.applyingProfile(profile), profile.title)
            }
        }
    }

    fun toggleDsp() {
        updateConfig(currentConfig.copy(enabled = !currentConfig.enabled))
    }

    /** Steps through the real presets (bypass excluded). Always leaves tuning switched on. */
    @Synchronized
    fun cycleProfile(step: Int) {
        val list = TuningProfile.values().filter { it != TuningProfile.BYPASS }
        val index = list.indexOfFirst { it.title == currentProfileName }
        val next = if (index < 0) {
            if (step > 0) 0 else list.lastIndex
        } else {
            Math.floorMod(index + step, list.size)
        }
        val profile = list[next]
        updateConfig(currentConfig.applyingProfile(profile), profile.title)
    }

    /** Name of the preset whose values equal [c], or null if the user has customised it. */
    private fun matchProfileTitle(c: DspConfig): String? {
        val byId = c.activeProfileId?.let { id ->
            TuningProfile.values().firstOrNull { it.acousticProfile?.id == id }?.title
                ?: AcousticProfileRepository.findById(id)?.name
        }
        if (byId != null) return byId

        return TuningProfile.values().firstOrNull { p ->
            c.eqEnabled &&
                p != TuningProfile.BYPASS &&
                kotlin.math.abs(c.highPassCutoffHz - p.config.highPassCutoffHz) < 0.5f &&
                kotlin.math.abs(c.bassGainDb - p.config.bassGainDb) < 0.05f &&
                kotlin.math.abs(c.boxinessCutDb - p.config.boxinessCutDb) < 0.05f &&
                kotlin.math.abs(c.vocalClarityDb - p.config.vocalClarityDb) < 0.05f &&
                kotlin.math.abs(c.trebleGainDb - p.config.trebleGainDb) < 0.05f &&
                kotlin.math.abs(c.loudnessGainDb - p.config.loudnessGainDb) < 0.05f
        }?.title
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
        } else if (newConfig.enabled) {
            currentProfileName = if (!newConfig.eqEnabled) "Boost only"
                                 else matchProfileTitle(newConfig) ?: "Custom"
        }

        applyChannel.trySend(Unit)

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

        saveConfig()
        updateNotification()
    }

    fun getActiveSessions(): List<Pair<Int, String>> {
        return activeEngines.keys.map { id -> id to (sessionPackages[id] ?: "Active Stream") }
    }

    @SuppressLint("BlockedPrivateApi")
    private fun extractSessionId(config: AudioPlaybackConfiguration): Int? {
        try {
            val method = config.javaClass.getMethod("getSessionId")
            val id = (method.invoke(config) as? Number)?.toInt()
            if (id != null && id > 0) return id
        } catch (_: Throwable) {}
        try {
            val field = config.javaClass.getDeclaredField("mSessionId")
            field.isAccessible = true
            val id = field.getInt(config)
            if (id > 0) return id
        } catch (_: Throwable) {}
        try {
            val match = DUMPSYS_CONFIG_REGEX.find(config.toString())
                ?: Regex("""sessionId:(\d+)""").find(config.toString())
            if (match != null) {
                val groupIdx = if (match.groupValues.size >= 4) 3 else 1
                return match.groupValues[groupIdx].toIntOrNull()
            }
        } catch (_: Throwable) {}
        return null
    }

    private fun extractClientUid(config: AudioPlaybackConfiguration): Int? {
        try {
            val method = config.javaClass.getMethod("getClientUid")
            val uid = (method.invoke(config) as? Number)?.toInt()
            if (uid != null && uid > 0) return uid
        } catch (_: Throwable) {}
        try {
            val field = config.javaClass.getDeclaredField("mClientUid")
            field.isAccessible = true
            val uid = field.getInt(config)
            if (uid > 0) return uid
        } catch (_: Throwable) {}
        try {
            val match = Regex("""u/pid:(\d+)""").find(config.toString())
                ?: Regex("""uid/pid:(\d+)""").find(config.toString())
            if (match != null) {
                return match.groupValues[1].toIntOrNull()
            }
        } catch (_: Throwable) {}
        return null
    }

    private fun extractSessionsFromConfigs(
        configs: List<AudioPlaybackConfiguration>,
        discovered: MutableMap<Int, String>
    ) {
        for (config in configs) {
            val sessionId = extractSessionId(config) ?: continue
            val uid = extractClientUid(config) ?: -1
            val pkg = if (uid > 0) resolvePackageName(uid) else "Media Player"
            if (isValidExternalSession(uid, sessionId, pkg)) {
                val isPlaying = try {
                    (config.javaClass.getMethod("isActive").invoke(config) as? Boolean)
                        ?: ((config.javaClass.getMethod("getPlayerState").invoke(config) as? Int) == 2)
                } catch (_: Throwable) {
                    true
                }
                if (isPlaying) {
                    discovered[sessionId] = pkg
                }
            }
        }
    }

    private fun registerPlaybackCallback() {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            playbackCallback = object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                    if (currentConfig.globalHookEnabled) {
                        serviceScope.launch {
                            val discovered = mutableMapOf<Int, String>()
                            extractSessionsFromConfigs(configs, discovered)
                            for ((sessionId, pkg) in discovered) {
                                missedScanCounts[sessionId] = 0
                                if (!activeEngines.containsKey(sessionId)) {
                                    attachSession(sessionId, pkg, SessionOrigin.AUTO_SNIFFER)
                                }
                            }
                            if (hasDumpPermission()) {
                                scanActiveSessions()
                            }
                        }
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
        scannerJob = serviceScope.launch {
            while (isActive && currentConfig.globalHookEnabled) {
                scanActiveSessions()
                // 4s polling detects new browser tabs and music tracks promptly
                delay(4000)
            }
        }
        Log.i(TAG, "Started background audio session scanner")
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

        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            extractSessionsFromConfigs(audioManager.activePlaybackConfigurations, discovered)
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading activePlaybackConfigurations", e)
        }

        if (hasDumpPermission()) {
            val cmd = if (java.io.File("/system/bin/dumpsys").exists()) "/system/bin/dumpsys audio" else "dumpsys audio"
            parseAudioDump(cmd, discovered)
        }

        // Attach newly discovered active sessions
        for ((sessionId, pkg) in discovered) {
            missedScanCounts[sessionId] = 0
            if (!activeEngines.containsKey(sessionId)) {
                attachSession(sessionId, pkg, SessionOrigin.AUTO_SNIFFER)
            }
        }

        // Detach sessions only after 4 consecutive missed scans to avoid transient audio drops during buffering
        val autoSniffed = activeEngines.keys.filter { id ->
            sessionOrigins[id] == SessionOrigin.AUTO_SNIFFER
        }
        for (id in autoSniffed) {
            if (!discovered.containsKey(id)) {
                val missed = (missedScanCounts[id] ?: 0) + 1
                missedScanCounts[id] = missed
                if (missed >= 4) {
                    missedScanCounts.remove(id)
                    detachSession(id)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "EvenLouder Audio Tuning Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Maintains micro-speaker dynamics tuning in the background"
            setShowBadge(false)
            setSound(null, null)      // Completely silent: no chimes or beeps on updates
            enableVibration(false)    // No vibration
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
        val enabled = currentConfig.enabled

        val togglePending = createActionPendingIntent(ACTION_TOGGLE_DSP, reqCode = 1)
        val prevPending = createActionPendingIntent(ACTION_PREV_PROFILE, reqCode = 2)
        val nextPending = createActionPendingIntent(ACTION_NEXT_PROFILE, reqCode = 3)

        val contentPending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (enabled) currentProfileName else "Tuning off"
        val text = when {
            !enabled -> "Original phone sound"
            count == 0 -> "Waiting for audio"
            else -> "Tuning $count stream${if (count == 1) "" else "s"}"
        }
        val boostText = if (currentConfig.volumeBoostDb > 0.05f) " · Boost ${"%+.1f".format(currentConfig.volumeBoostDb)} dB" else ""
        val details = if (enabled) {
            "Bass ${"%+.1f".format(currentConfig.bassGainDb)} · " +
                "Voice ${"%+.1f".format(currentConfig.vocalClarityDb)} · " +
                "Treble ${"%+.1f".format(currentConfig.trebleGainDb)} · " +
                "Loudness ${"%+.1f".format(currentConfig.loudnessGainDb)} dB$boostText\n" +
                "Use Previous / Next to switch profile."
        } else {
            "Tap Turn on to bring tuning back."
        }

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_waveform)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText("EvenLouder")
            .setContentIntent(contentPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setColor(0xFF00796B.toInt())
            .setStyle(Notification.BigTextStyle().bigText(details))
            .addAction(Notification.Action.Builder(null, if (enabled) "Turn off" else "Turn on", togglePending).build())
            .addAction(Notification.Action.Builder(null, "Previous", prevPending).build())
            .addAction(Notification.Action.Builder(null, "Next", nextPending).build())

        if (Build.VERSION.SDK_INT >= 31) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    @Volatile
    private var notifyJob: Job? = null

    /** Debounced: slider drags would otherwise post dozens of notification updates per second. */
    private fun updateNotification() {
        notifyJob?.cancel()
        notifyJob = serviceScope.launch {
            delay(250)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification())
            // Keep home-screen widget and quick settings tile in sync with the same state
            EvenLouderWidget.refresh(applicationContext)
            EvenLouderTileService.requestRefresh(applicationContext)
        }
    }

    private fun loadSavedConfig() {
        val p = getSharedPreferences("evenlouder_prefs", MODE_PRIVATE)
        if (!p.contains("hpf")) return
        val d = DspConfig()
        currentConfig = DspConfig(
            enabled = p.getBoolean("enabled", d.enabled),
            eqEnabled = p.getBoolean("eq_on", d.eqEnabled),
            highPassCutoffHz = p.getFloat("hpf", d.highPassCutoffHz),
            bassGainDb = p.getFloat("bass", d.bassGainDb),
            boxinessCutDb = p.getFloat("box", d.boxinessCutDb),
            vocalClarityDb = p.getFloat("vocal", d.vocalClarityDb),
            trebleGainDb = p.getFloat("treble", d.trebleGainDb),
            loudnessGainDb = p.getFloat("loudness", d.loudnessGainDb),
            limiterCeilingDb = p.getFloat("limiter", d.limiterCeilingDb),
            volumeBoostDb = p.getFloat("boost", d.volumeBoostDb),
            globalHookEnabled = p.getBoolean("hook", d.globalHookEnabled),
            activeProfileId = p.getString("active_profile_id", d.activeProfileId)
        )
        currentProfileName = p.getString("profile", currentProfileName) ?: currentProfileName
    }

    private fun saveConfig() {
        val c = currentConfig
        getSharedPreferences("evenlouder_prefs", MODE_PRIVATE).edit()
            .putBoolean("enabled", c.enabled)
            .putBoolean("eq_on", c.eqEnabled)
            .putFloat("hpf", c.highPassCutoffHz)
            .putFloat("bass", c.bassGainDb)
            .putFloat("box", c.boxinessCutDb)
            .putFloat("vocal", c.vocalClarityDb)
            .putFloat("treble", c.trebleGainDb)
            .putFloat("loudness", c.loudnessGainDb)
            .putFloat("limiter", c.limiterCeilingDb)
            .putFloat("boost", c.volumeBoostDb)
            .putBoolean("hook", c.globalHookEnabled)
            .putString("profile", currentProfileName)
            .putString("active_profile_id", c.activeProfileId)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSessionScanner()
        try {
            playbackCallback?.let {
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                audioManager.unregisterAudioPlaybackCallback(it)
            }
        } catch (_: Exception) {}

        applyChannel.close()
        serviceScope.cancel()
        for ((_, engine) in activeEngines) {
            engine.release()
        }
        activeEngines.clear()
        sessionPackages.clear()
        sessionOrigins.clear()
        missedScanCounts.clear()
        failedAttempts.clear()
        failedAttemptCounts.clear()
        instance = null
        Log.i(TAG, "AudioEngineService destroyed")
    }
}

/**
 * Receives button taps from the notification, the home-screen widget and similar surfaces.
 * Broadcasts run immediately and bypass background service launch restrictions.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = AudioEngineService.instance
        if (service == null) {
            val serviceIntent = Intent(context, AudioEngineService::class.java).apply {
                action = intent.action
                putExtras(intent)
            }
            try {
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                // Background start can be denied by the OS; never crash from a tap
                Log.w("NotificationActionReceiver", "Could not start service", e)
            }
            return
        }
        service.handleControlAction(intent.action, intent.getStringExtra(AudioEngineService.EXTRA_PROFILE_NAME))
    }
}
