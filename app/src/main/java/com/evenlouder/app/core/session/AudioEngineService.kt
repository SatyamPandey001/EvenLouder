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
 * Features automatic active session discovery, clean physical bypass, and silent notifications.
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

        val SESSION_SNIFFER_REGEX = Regex("""(\d+)\s+yes\s+(\d+)/\s*(\d+)\s+(\d+)""")

        @Volatile
        var instance: AudioEngineService? = null
            private set
    }

    inner class LocalBinder : Binder() {
        val service: AudioEngineService get() = this@AudioEngineService
    }

    private val binder = LocalBinder()
    private val activeEngines = ConcurrentHashMap<Int, DynamicsEngine>()
    private val sessionPackages = ConcurrentHashMap<Int, String>()

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
    fun attachSession(sessionId: Int, pkg: String = "Internal") {
        if (!currentConfig.enabled) return // Do not attach effects if user turned DSP OFF
        if (activeEngines.containsKey(sessionId)) return

        try {
            val engine = DynamicsEngine(audioSessionId = sessionId, channelCount = 2)
            engine.applyConfig(currentConfig)
            activeEngines[sessionId] = engine
            sessionPackages[sessionId] = pkg
            updateNotification()
            Log.i(TAG, "Attached DSP to session $sessionId ($pkg). Total active: ${activeEngines.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed attaching session $sessionId", e)
        }
    }

    @Synchronized
    fun detachSession(sessionId: Int) {
        activeEngines.remove(sessionId)?.let { engine ->
            engine.release()
            sessionPackages.remove(sessionId)
            updateNotification()
            Log.i(TAG, "Detached DSP from session $sessionId. Remaining: ${activeEngines.size}")
        }
    }

    @Synchronized
    fun updateConfig(newConfig: DspConfig, profileTitle: String? = null) {
        val wasGlobal = currentConfig.globalHookEnabled
        val wasEnabled = currentConfig.enabled
        currentConfig = newConfig
        if (profileTitle != null) {
            currentProfileName = profileTitle
        }

        if (!newConfig.enabled && wasEnabled) {
            // Physically release and clear all DSP engines from AudioFlinger
            // so the phone speaker returns 100% to pure, unmodified stock audio!
            for ((_, engine) in activeEngines) {
                engine.release()
            }
            activeEngines.clear()
            Log.i(TAG, "DSP TURNED OFF: Physically released all effects from hardware channels")
        } else if (newConfig.enabled && !wasEnabled) {
            // Re-attached to active media sessions
            Log.i(TAG, "DSP TURNED ON: Re-scanning and attaching to active sessions")
            if (newConfig.globalHookEnabled) {
                serviceScope.launch { scanActiveSessions() }
            }
        } else if (newConfig.enabled) {
            // Apply parameter updates to existing active engines
            for ((_, engine) in activeEngines) {
                engine.applyConfig(newConfig)
            }
        }

        if (newConfig.globalHookEnabled && !wasGlobal) {
            startSessionScanner()
        } else if (!newConfig.globalHookEnabled && wasGlobal) {
            stopSessionScanner()
            val external = activeEngines.keys.filter { id ->
                sessionPackages[id] != "Internal Acoustic Loop"
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
                    if (currentConfig.globalHookEnabled && currentConfig.enabled) {
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
        scannerJob = serviceScope.launch {
            while (isActive && currentConfig.globalHookEnabled) {
                if (currentConfig.enabled) {
                    scanActiveSessions()
                }
                delay(3000)
            }
        }
        Log.i(TAG, "Started background audio session scanner")
    }

    private fun stopSessionScanner() {
        scannerJob?.cancel()
        scannerJob = null
        Log.i(TAG, "Stopped background audio session scanner")
    }

    private fun scanActiveSessions() {
        if (!currentConfig.enabled) return

        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec("dumpsys media.audio_flinger")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val discovered = mutableSetOf<Int>()

            reader.useLines { lines ->
                for (line in lines) {
                    val match = SESSION_SNIFFER_REGEX.find(line) ?: continue
                    val uid = match.groupValues[3].toIntOrNull() ?: continue
                    val sessionId = match.groupValues[4].toIntOrNull() ?: continue

                    if (uid == android.os.Process.myUid() || uid == 1041 || uid == 1000) continue
                    if (sessionId <= 0) continue

                    discovered.add(sessionId)
                    if (!activeEngines.containsKey(sessionId)) {
                        val pkgName = packageManager.getPackagesForUid(uid)?.firstOrNull() ?: "App (UID $uid)"
                        attachSession(sessionId, pkgName)
                    }
                }
            }

            val stale = activeEngines.keys.filter { id ->
                sessionPackages[id] != "Internal Acoustic Loop" && !discovered.contains(id)
            }
            for (id in stale) {
                detachSession(id)
            }
        } catch (_: Exception) {
        } finally {
            try {
                process?.destroy()
            } catch (_: Exception) {}
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
            .addAction(Notification.Action.Builder(null, "Dolby 3D", dolbyPendingIntent).build())
            .addAction(Notification.Action.Builder(null, "MaxxBass", maxxBassPendingIntent).build())
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
