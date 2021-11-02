package io.github.lee0701.converter

object CharacterSet {
    fun isHanja(c: Char) = c.toInt() in 0x4E00 .. 0x62FF
            || c.toInt() in 0x6300 .. 0x77FF || c.toInt() in 0x7800 .. 0x8CFF
            || c.toInt() in 0x8D00 .. 0x9FFF || c.toInt() in 0x3400 .. 0x4DBF

    fun isHangul(c: Char) = c.toInt() in 0xAC00 .. 0xD7AF
            || c.toInt() in 0x1100 .. 0x11FF || c.toInt() in 0xA960 .. 0xA97F
            || c.toInt() in 0xD7B0 .. 0xD7FF || c.toInt() in 0x3130 .. 0x318F
}