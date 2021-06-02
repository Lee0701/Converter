package io.github.lee0701.converter.candidates

import android.graphics.Color
import androidx.core.graphics.ColorUtils

object CandidateWindowColor {
    const val DEFAULT = (0xFFFAFAFA).toInt()
    const val GBOARD = (0xFFE8EAED).toInt()

    fun of(name: String, custom: Int = DEFAULT) = when(name) {
        "default" -> DEFAULT
        "gboard" -> GBOARD
        "custom" -> custom
        else -> DEFAULT
    }

    fun textColorOf(color: Int): Int {
        val dark = ColorUtils.calculateLuminance(color) < 0.5
        return if(dark) Color.WHITE else Color.BLACK
    }

    fun extraColorOf(color: Int): Int {
        val dark = ColorUtils.calculateLuminance(color) < 0.5
        return if(dark) Color.LTGRAY else Color.DKGRAY
    }

}