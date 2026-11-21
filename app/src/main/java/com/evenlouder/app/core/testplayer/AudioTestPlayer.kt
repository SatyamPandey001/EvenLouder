package com.evenlouder.app.core.testplayer

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.evenlouder.app.R

/**
 * Self-contained audio player for immediate on-device acoustic auditioning.
 * Uses native MediaPlayer loading bundled acoustic audio with explicit speaker routing.
 */
class AudioTestPlayer(private val context: Context) {

    companion object {
        private const val TAG = "AudioTestPlayer"
    }

    private var mediaPlayer: MediaPlayer? = null
    var isPlaying: Boolean = false
        private set

    val audioSessionId: Int
        get() = mediaPlayer?.audioSessionId ?: 0

    fun start(onPrepared: (sessionId: Int) -> Unit = {}) {
        if (isPlaying) stop()

        try {
            val player = MediaPlayer.create(context, R.raw.sample_acoustic) ?: run {
                Log.e(TAG, "Failed creating MediaPlayer from R.raw.sample_acoustic")
                return
            }

            // Route explicitly to built-in speaker so USB submix cannot divert the audio
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val speakerDevice = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            speakerDevice?.let {
                player.preferredDevice = it
                Log.d(TAG, "Assigned preferredDevice: Built-in Speaker")
            }

            player.isLooping = true
            player.setVolume(1.0f, 1.0f)
            player.start()

            mediaPlayer = player
            isPlaying = true
            Log.i(TAG, "Playing sample_acoustic on session ${player.audioSessionId}")
            onPrepared(player.audioSessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting test audio", e)
            isPlaying = false
        }
    }

    fun stop() {
        isPlaying = false
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping test audio", e)
        } finally {
            mediaPlayer = null
        }
    }
}
