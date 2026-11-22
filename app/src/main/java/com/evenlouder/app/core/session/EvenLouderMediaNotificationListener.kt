package com.evenlouder.app.core.session

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Fallback Media Session & Notification Listener.
 * Allows users to enable media playback session discovery via standard Android System Settings
 * (Special App Access -> Notification Listener Access) without requiring ADB or a computer.
 */
class EvenLouderMediaNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "MediaNotifListener"

        fun isGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat?.contains(context.packageName) == true
        }

        fun openSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open notification listener settings", e)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification Listener connected. Scanning active media sessions...")
        scanMediaSessions()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            if (it.notification.category == android.app.Notification.CATEGORY_TRANSPORT ||
                it.notification.extras.containsKey(android.app.Notification.EXTRA_MEDIA_SESSION)
            ) {
                scanMediaSessions()
            }
        }
    }

    private fun scanMediaSessions() {
        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            val componentName = ComponentName(this, EvenLouderMediaNotificationListener::class.java)
            val controllers: List<MediaController> = mediaSessionManager.getActiveSessions(componentName)

            for (controller in controllers) {
                val pkg = controller.packageName ?: continue
                val playbackState = controller.playbackState
                if (playbackState != null && playbackState.state == android.media.session.PlaybackState.STATE_PLAYING) {
                    Log.d(TAG, "Active media session detected via Notification Listener: $pkg")
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification listener permission missing for getActiveSessions", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning media sessions", e)
        }
    }
}
