package com.evenlouder.app.core.session

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.evenlouder.app.R

/**
 * Quick Settings tile (notification shade): tap to switch tuning on or off.
 * Active = tuning on (subtitle shows the profile), inactive = original sound.
 */
class EvenLouderTileService : TileService() {

    override fun onStartListening() {
        refreshTile()
    }

    override fun onClick() {
        val service = AudioEngineService.instance
        if (service != null) {
            service.toggleDsp()
        } else {
            try {
                startForegroundService(
                    Intent(this, AudioEngineService::class.java).setAction(AudioEngineService.ACTION_TOGGLE_DSP)
                )
            } catch (e: Exception) {
                Log.w("EvenLouderTile", "Could not start service from tile", e)
            }
        }
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val service = AudioEngineService.instance
        val prefs = getSharedPreferences("evenlouder_prefs", MODE_PRIVATE)
        val enabled = service?.currentConfig?.enabled ?: prefs.getBoolean("enabled", true)
        val profile = service?.currentProfileName ?: prefs.getString("profile", null) ?: "EvenLouder"

        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "EvenLouder"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_waveform)
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = if (enabled) profile else "Bypassed"
        }
        tile.updateTile()
    }

    companion object {
        /** Asks the system to call onStartListening again so the tile redraws. */
        fun requestRefresh(context: Context) {
            try {
                requestListeningState(context, ComponentName(context, EvenLouderTileService::class.java))
            } catch (_: Exception) {
                // Tile not added; nothing to refresh
            }
        }
    }
}
