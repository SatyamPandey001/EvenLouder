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

        val action = intent.action ?: return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
            ?.take(64)                       // untrusted input from other apps
            ?.takeIf { it.isNotBlank() }
            ?: "External Media Player"

        if (sessionId <= 0) return

        val service = AudioEngineService.instance
        if (service == null) {
            if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION) {
                val serviceIntent = Intent(context, AudioEngineService::class.java).apply {
                    this.action = action
                    putExtras(intent)
                }
                try {
                    context.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not start service from audio session broadcast", e)
                }
            }
            return
        }

        // Guard: Do not intercept external apps unless user explicitly enabled global hooking
        if (!service.currentConfig.globalHookEnabled) {
            Log.d(TAG, "Global hook disabled; ignoring broadcast from external app")
            return
        }

        when (action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> service.attachSession(sessionId, packageName, AudioEngineService.SessionOrigin.BROADCAST)
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> service.detachSession(sessionId)
        }
    }
}
