package com.evenlouder.app.core.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log

/**
 * Intercepts Android audio effect session broadcast intents.
 * Only forwards sessions if the user has explicitly enabled third-party player hooking.
 */
class AudioSessionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AudioSessionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val service = AudioEngineService.instance ?: return
        // Guard: Do not intercept external apps unless user explicitly enabled global hooking
        if (!service.currentConfig.globalHookEnabled) {
            Log.d(TAG, "Global hook disabled; ignoring broadcast from external app")
            return
        }

        val action = intent.action ?: return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "External Media Player"

        if (sessionId <= 0) return

        val serviceIntent = Intent(context, AudioEngineService::class.java).apply {
            putExtra(AudioEngineService.EXTRA_SESSION_ID, sessionId)
            putExtra(AudioEngineService.EXTRA_CLIENT_PKG, packageName)
            this.action = when (action) {
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> AudioEngineService.ACTION_ATTACH_SESSION
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> AudioEngineService.ACTION_DETACH_SESSION
                else -> null
            }
        }

        if (serviceIntent.action != null) {
            try {
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed forwarding session intent", e)
            }
        }
    }
}
