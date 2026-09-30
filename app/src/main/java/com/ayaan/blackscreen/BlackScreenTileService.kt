package com.ayaan.blackscreen

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class BlackScreenTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (VolumeKeyAccessibilityService.isOverlayActive) {
            VolumeKeyAccessibilityService.requestHideOverlay()
        } else {
            VolumeKeyAccessibilityService.requestShowOverlay()
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.let { tile ->
            val active = VolumeKeyAccessibilityService.isOverlayActive
            tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = if (active) "Screen: Off" else "Screen: On"
            tile.updateTile()
        }
    }
}
