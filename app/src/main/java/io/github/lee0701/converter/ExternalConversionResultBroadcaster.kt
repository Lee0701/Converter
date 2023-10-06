package io.github.lee0701.converter

import android.content.Context
import android.content.Intent
import io.github.lee0701.converter.engine.Candidate

object ExternalConversionResultBroadcaster {
    const val ACTION_CONVERT_TEXT_RESULT = "io.github.lee0701.mboard.intent.action.CONVERT_TEXT_RESULT"
    const val PERMISSION_RECEIVE_CONVERTED_TEXT = "io.github.lee0701.mboard.permission.RECEIVE_CONVERTED_TEXT"
    const val EXTRA_TEXT = "io.github.lee0701.mboard.intent.extra.TEXT"

    fun broadcast(context: Context, candidates: List<Candidate>) {
        val intent = Intent().apply {
            action = ACTION_CONVERT_TEXT_RESULT
            val text = candidates.map { (hangul, hanja, extra) -> "$hangul\t$hanja\t$extra" }.toTypedArray()
            putExtra(EXTRA_TEXT, text)
        }
        context.sendBroadcast(intent, PERMISSION_RECEIVE_CONVERTED_TEXT)
    }
}