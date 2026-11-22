package com.evenlouder.app.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.session.AudioEngineService
import com.evenlouder.app.core.testplayer.AudioTestPlayer
import com.evenlouder.app.ui.screens.DashboardScreen
import com.evenlouder.app.ui.theme.EvenLouderTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var audioEngineService: AudioEngineService? = null
    private val testPlayer by lazy { AudioTestPlayer(this) }
    private var isAppReady = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? AudioEngineService.LocalBinder
            audioEngineService = localBinder?.service
            isAppReady = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioEngineService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isAppReady }
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Start & bind AudioEngineService
        val serviceIntent = Intent(this, AudioEngineService::class.java)
        startForegroundService(serviceIntent)
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            var dspConfig by remember { mutableStateOf(DspConfig()) }
            var isPlayingTestAudio by remember { mutableStateOf(false) }
            var activeSessions by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
            var isDumpPermissionGranted by remember { mutableStateOf(false) }

            // Sync service state and dismiss splash as soon as connected (fallback 600ms)
            LaunchedEffect(Unit) {
                val startTime = System.currentTimeMillis()
                while (audioEngineService == null && (System.currentTimeMillis() - startTime) < 600) {
                    delay(20)
                }
                audioEngineService?.let { service ->
                    dspConfig = service.currentConfig
                    activeSessions = service.getActiveSessions()
                    isDumpPermissionGranted = service.hasDumpPermission()
                }
                isAppReady = true

                while (true) {
                    audioEngineService?.let { service ->
                        activeSessions = service.getActiveSessions()
                        dspConfig = service.currentConfig
                        isDumpPermissionGranted = service.hasDumpPermission()
                    }
                    delay(1000)
                }
            }

            EvenLouderTheme(darkTheme = isDarkTheme) {
                DashboardScreen(
                    config = dspConfig,
                    activeSessions = activeSessions,
                    isTestAudioPlaying = isPlayingTestAudio,
                    isDarkTheme = isDarkTheme,
                    isDumpPermissionGranted = isDumpPermissionGranted,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onConfigChange = { newConfig ->
                        dspConfig = newConfig
                        audioEngineService?.updateConfig(newConfig)
                    },
                    onTestAudioToggle = {
                        if (isPlayingTestAudio) {
                            val sessionId = testPlayer.audioSessionId
                            testPlayer.stop()
                            if (sessionId > 0) {
                                audioEngineService?.detachSession(sessionId)
                            }
                            isPlayingTestAudio = false
                        } else {
                            testPlayer.start { sessionId ->
                                if (sessionId > 0) {
                                    audioEngineService?.attachSession(sessionId, "Internal Acoustic Loop")
                                }
                            }
                            isPlayingTestAudio = true
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        testPlayer.stop()
        try {
            unbindService(serviceConnection)
        } catch (_: Exception) {}
    }
}
