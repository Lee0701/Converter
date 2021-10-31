package io.github.lee0701.converter.candidates

import android.graphics.Color
import androidx.core.graphics.ColorUtils

object CandidateWindowColor {
    const val ALPHA = 0.54f

    const val DEFAULT = (0xfffafafa).toInt()
    const val GBOARD_LIGHT_FLAT = (0xfff2f3f5).toInt()
    const val GBOARD_DARK_FLAT = (0xff292e32).toInt()
    const val GBOARD_LIGHT_BORDER = (0xffe8eaed).toInt()
    const val GBOARD_DARK_BORDER = (0xff292e32).toInt()

    fun of(name: String, custom: Int = DEFAULT) = when(name) {
        "default" -> DEFAULT
        "gboard_light_flat" -> GBOARD_LIGHT_FLAT
        "gboard_dark_flat" -> GBOARD_DARK_FLAT
        "gboard_light_border" -> GBOARD_LIGHT_BORDER
        "gboard_dark_border" -> GBOARD_DARK_BORDER
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