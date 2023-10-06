package io.github.lee0701.converter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ExternalConversionBroadcastReceiver: BroadcastReceiver() {
    var broadcastReceived = false

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return
        broadcastReceived = true
        val text = intent.getStringExtra(EXTRA_TEXT) ?: return
        ConverterAccessibilityService.INSTANCE?.externalConvert(text)
    }

    companion object {
        const val ACTION_CONVERT_TEXT = "io.github.lee0701.mboard.intent.action.CONVERT_TEXT"
        const val PERMISSION_CONVERT_TEXT = "io.github.lee0701.mboard.permission.CONVERT_TEXT"
        const val EXTRA_TEXT = "io.github.lee0701.mboard.intent.extra.TEXT"
    }
}