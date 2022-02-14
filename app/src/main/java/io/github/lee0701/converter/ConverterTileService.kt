package io.github.lee0701.converter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import io.github.lee0701.converter.assistant.InputAssistantWindow

@RequiresApi(Build.VERSION_CODES.N)
class ConverterTileService: TileService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var inputAssistantWindow: InputAssistantWindow

    override fun onStartListening() {
        val tile = qsTile ?: return
        inputAssistantWindow = InputAssistantWindow(this)
        tile.state = if(ConverterAccessibilityService.INSTANCE != null) Tile.STATE_ACTIVE else Tile.STATE_UNAVAILABLE
        tile.updateTile()
    }

    override fun onClick() {
        val accessibilityService = ConverterAccessibilityService.INSTANCE ?: return
        inputAssistantWindow.show { text ->
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(text, text)
            inputAssistantWindow.destroy()
            clipboard.setPrimaryClip(clip)
            handler.postDelayed({ accessibilityService.pasteClipboard() }, 300)
        }
    }
}