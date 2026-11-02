package com.adheesh.gid.feature.capture.ui

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.adheesh.gid.R

// ── Quick Settings tile 'GID capture' (SPEC §2.11) ───────────────────────────
// A button in the pull-down shade. Tapping it closes the shade and opens the
// small capture menu (Camera, Photo, Paste) over whatever is on screen.
class CaptureTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE   // an action, not an on/off switch
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        // On the lock screen, ask to unlock first: captures are private.
        if (isLocked) unlockAndRun { openMenu() } else openMenu()
    }

    // Lint flags the Intent overload even behind the version check; before
    // Android 14 it is the only way to open an activity from a tile.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openMenu() {
        val intent = QuickCaptureActivity.intent(this, QuickCaptureActivity.MODE_MENU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")   // the Intent version is the only one before Android 14
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        /** Android 13+: asks the system to show 'Add tile?' for this tile. */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun requestAdd(context: Context, onResult: (added: Boolean) -> Unit) {
            val manager = context.getSystemService(StatusBarManager::class.java) ?: return onResult(false)
            manager.requestAddTileService(
                ComponentName(context, CaptureTileService::class.java),
                context.getString(R.string.tile_label),
                Icon.createWithResource(context, R.drawable.ic_widget_camera),
                context.mainExecutor,
            ) { result ->
                onResult(
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                        result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                )
            }
        }
    }
}
