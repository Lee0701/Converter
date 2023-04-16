package io.github.lee0701.converter.library

object CharacterSet {
    fun isHanja(c: Char) = c.code in 0x4E00 .. 0x62FF
            || c.code in 0x6300 .. 0x77FF || c.code in 0x7800 .. 0x8CFF
            || c.code in 0x8D00 .. 0x9FFF || c.code in 0x3400 .. 0x4DBF

    fun isHangul(c: Char) = c.code in 0xAC00 .. 0xD7AF
            || c.code in 0x1100 .. 0x11FF || c.code in 0xA960 .. 0xA97F
            || c.code in 0xD7B0 .. 0xD7FF || c.code in 0x3130 .. 0x318F
}