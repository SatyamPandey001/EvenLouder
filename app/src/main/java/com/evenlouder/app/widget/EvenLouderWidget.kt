package com.evenlouder.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.evenlouder.app.R
import com.evenlouder.app.core.dsp.TuningProfile
import com.evenlouder.app.core.session.AudioEngineService
import com.evenlouder.app.core.session.NotificationActionReceiver
import com.evenlouder.app.ui.MainActivity

/**
 * Home-screen widget: power toggle, current profile, previous / next profile.
 *
 * It reads the state the service saved to SharedPreferences, so it renders correctly even
 * when the audio service is not running. Taps go to [NotificationActionReceiver], which
 * starts the service if needed.
 */
class EvenLouderWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = buildViews(context)
        appWidgetIds.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        /** Re-renders every placed widget. Called by the service whenever state changes. */
        fun refresh(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, EvenLouderWidget::class.java))
                if (ids.isEmpty()) return
                manager.updateAppWidget(ids, buildViews(context))
            } catch (_: Exception) {
                // Widget host unavailable; nothing to do
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences("evenlouder_prefs", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("enabled", true)
            val profile = prefs.getString("profile", null) ?: TuningProfile.CINEMATIC.title

            return RemoteViews(context.packageName, R.layout.widget_evenlouder).apply {
                setTextViewText(R.id.widget_status, if (enabled) "Tuning on" else "Bypassed")
                setTextViewText(R.id.widget_profile, profile)
                setInt(
                    R.id.widget_power,
                    "setBackgroundResource",
                    if (enabled) R.drawable.widget_btn_on else R.drawable.widget_btn_off
                )
                setContentDescription(
                    R.id.widget_power,
                    if (enabled) "Turn tuning off" else "Turn tuning on"
                )

                setOnClickPendingIntent(R.id.widget_power, action(context, AudioEngineService.ACTION_TOGGLE_DSP, 10))
                setOnClickPendingIntent(R.id.widget_prev, action(context, AudioEngineService.ACTION_PREV_PROFILE, 11))
                setOnClickPendingIntent(R.id.widget_next, action(context, AudioEngineService.ACTION_NEXT_PROFILE, 12))

                val open = PendingIntent.getActivity(
                    context, 13, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_info, open)
            }
        }

        private fun action(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
