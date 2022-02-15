package io.github.lee0701.converter

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import io.github.lee0701.converter.assistant.InputAssistantLauncherWindow
import io.github.lee0701.converter.assistant.InputAssistantWindow

@RequiresApi(Build.VERSION_CODES.N)
class ConverterTileService: TileService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var inputAssistantWindow: InputAssistantWindow
    private lateinit var inputAssistantLauncherWindow: InputAssistantLauncherWindow

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }

    override fun onDestroy() {
        super.onDestroy()
        INSTANCE = null
    }

    override fun onStartListening() {
        val tile = qsTile ?: return
        inputAssistantWindow = InputAssistantWindow(this)
        inputAssistantLauncherWindow = InputAssistantLauncherWindow(this)
        tile.state = if(ConverterAccessibilityService.INSTANCE != null) Tile.STATE_ACTIVE else Tile.STATE_UNAVAILABLE
        tile.updateTile()
    }

    override fun onClick() {
        val accessibilityService = ConverterAccessibilityService.INSTANCE ?: return
//        inputAssistantLauncherWindow.show()
        inputAssistantWindow.show { text ->
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(text, text)
            inputAssistantWindow.hide()
            clipboard.setPrimaryClip(clip)
            handler.postDelayed({ accessibilityService.pasteClipboard() }, 300)
        }
    }

    fun closeWindow() {
        inputAssistantWindow.hide()
    }

    companion object {
        var INSTANCE: ConverterTileService? = null
    }
}