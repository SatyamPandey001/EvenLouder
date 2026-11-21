package com.evenlouder.app.core.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.dsp.DynamicsEngine
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * Foreground Service maintaining hardware-safe DynamicsProcessing sessions.
 * Features automatic active session discovery for third-party media players.
 */
class AudioEngineService : Service() {

    companion object {
        private const val TAG = "AudioEngineService"
        private const val CHANNEL_ID = "evenlouder_audio_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_ATTACH_SESSION = "com.evenlouder.ACTION_ATTACH_SESSION"
        const val ACTION_DETACH_SESSION = "com.evenlouder.ACTION_DETACH_SESSION"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_CLIENT_PKG = "extra_client_pkg"

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

    var currentConfig: DspConfig = DspConfig()
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerPlaybackCallback()
        Log.i(TAG, "AudioEngineService started in foreground")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val sessionId = it.getIntExtra(EXTRA_SESSION_ID, 0)
            val pkg = it.getStringExtra(EXTRA_CLIENT_PKG) ?: "Unknown"

            when (it.action) {
                ACTION_ATTACH_SESSION -> if (sessionId > 0) attachSession(sessionId, pkg)
                ACTION_DETACH_SESSION -> if (sessionId > 0) detachSession(sessionId)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    @Synchronized
    fun attachSession(sessionId: Int, pkg: String = "Internal") {
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

    fun updateConfig(newConfig: DspConfig) {
        val wasGlobal = currentConfig.globalHookEnabled
        currentConfig = newConfig

        for ((_, engine) in activeEngines) {
            engine.applyConfig(newConfig)
        }

        if (newConfig.globalHookEnabled && !wasGlobal) {
            startSessionScanner()
        } else if (!newConfig.globalHookEnabled && wasGlobal) {
            stopSessionScanner()
            // Detach external players when global hook is turned off
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
        scannerJob = serviceScope.launch {
            while (isActive && currentConfig.globalHookEnabled) {
                scanActiveSessions()
                delay(1500)
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
        try {
            val process = Runtime.getRuntime().exec("dumpsys media.audio_flinger")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val regex = Regex("""(\d+)\s+yes\s+(\d+)/\s*(\d+)\s+(\d+)""")
            val discovered = mutableSetOf<Int>()

            reader.useLines { lines ->
                for (line in lines) {
                    val match = regex.find(line) ?: continue
                    val uid = match.groupValues[3].toIntOrNull() ?: continue
                    val sessionId = match.groupValues[4].toIntOrNull() ?: continue

                    // Ignore self and audioserver
                    if (uid == android.os.Process.myUid() || uid == 1041 || uid == 1000) continue
                    if (sessionId <= 0) continue

                    discovered.add(sessionId)
                    if (!activeEngines.containsKey(sessionId)) {
                        val pkgName = packageManager.getPackagesForUid(uid)?.firstOrNull() ?: "App (UID $uid)"
                        attachSession(sessionId, pkgName)
                    }
                }
            }

            // Clean up any external sessions that have stopped playing
            val stale = activeEngines.keys.filter { id ->
                sessionPackages[id] != "Internal Acoustic Loop" && !discovered.contains(id)
            }
            for (id in stale) {
                detachSession(id)
            }
        } catch (_: Exception) {
            // Silently ignore if dumpsys permission not granted yet
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
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val count = activeEngines.size
        val statusText = if (currentConfig.enabled) {
            "DSP Active on $count stream(s) • Limiter: -0.5 dBFS"
        } else {
            "DSP Bypassed"
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("EvenLouder Hardware Audio Engine")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
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
