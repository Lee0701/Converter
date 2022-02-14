package io.github.lee0701.converter

import java.text.Normalizer

object Hangul {

    fun combineJamo(a: Char, b: Char): String {
        return COMBINE_MAP[a to b]?.toString() ?: "$a$b"
    }

    fun splitJamo(char: Char): Pair<Char, Char>? {
        return REVERSE_COMBINE_MAP[char]
    }

    fun toCompat(char: Char): Char {
        val index = CONVERT_JAMO.indexOf(char)
        if(index < 0 || char == ' ') return char
        return COMPAT_JAMO[index]
    }

    fun toStandardInitial(char: Char): Char? {
        val index = COMPAT_CONSONANT.indexOf(char)
        if(index < 0) return null
        val result = CONVERT_INITIAL[index]
        if(result == ' ') return null
        return result
    }

    fun toStandardMedial(char: Char): Char? {
        val index = COMPAT_VOWEL.indexOf(char)
        if(index < 0) return null
        val result = CONVERT_MEDIAL[index]
        if(result == ' ') return null
        return result
    }

    fun toStandardFinal(char: Char): Char? {
        val index = COMPAT_CONSONANT.indexOf(char)
        if(index < 0) return null
        val result = CONVERT_FINAL[index]
        if(result == ' ') return null
        return result
    }

    private val COMBINE_MAP: Map<Pair<Char, Char>, Char> = mapOf(
        'ᄀ' to 'ᄀ' to 'ᄁ',
        'ᄃ' to 'ᄃ' to 'ᄄ',
        'ᄇ' to 'ᄇ' to 'ᄈ',
        'ᄉ' to 'ᄉ' to 'ᄊ',
        'ᄌ' to 'ᄌ' to 'ᄍ',
        'ᅩ' to 'ᅡ' to 'ᅪ',
        'ᅩ' to 'ᅢ' to 'ᅫ',
        'ᅩ' to 'ᅵ' to 'ᅬ',
        'ᅮ' to 'ᅥ' to 'ᅯ',
        'ᅮ' to 'ᅦ' to 'ᅰ',
        'ᅮ' to 'ᅵ' to 'ᅱ',
        'ᅳ' to 'ᅵ' to 'ᅴ',
        'ᆨ' to 'ᆨ' to 'ᆩ',
        'ᆨ' to 'ᆺ' to 'ᆪ',
        'ᆫ' to 'ᆽ' to 'ᆬ',
        'ᆫ' to 'ᇂ' to 'ᆭ',
        'ᆯ' to 'ᆨ' to 'ᆰ',
        'ᆯ' to 'ᆷ' to 'ᆱ',
        'ᆯ' to 'ᆸ' to 'ᆲ',
        'ᆯ' to 'ᆺ' to 'ᆳ',
        'ᆯ' to 'ᇀ' to 'ᆴ',
        'ᆯ' to 'ᇁ' to 'ᆵ',
        'ᆯ' to 'ᇂ' to 'ᆶ',
        'ᆸ' to 'ᆺ' to 'ᆹ',
        'ᆺ' to 'ᆺ' to 'ᆻ',
        //'' to '' to '',
    )

    private val REVERSE_COMBINE_MAP = COMBINE_MAP.map { (k, v) -> v to k }.toMap()

    private const val COMPAT_CONSONANT = "ㄱㄲㄳㄴㄵㄶㄷㄸㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅃㅄㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    private const val COMPAT_VOWEL = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"

    private const val CONVERT_INITIAL = "ᄀᄁ ᄂ  ᄃᄄᄅ       ᄆᄇᄈ ᄉᄊᄋᄌᄍᄎᄏᄐᄑᄒ"
    private const val CONVERT_MEDIAL = "ᅡᅢᅣᅤᅥᅦᅧᅨᅩᅪᅫᅬᅭᅮᅯᅰᅱᅲᅳᅴᅵ"
    private const val CONVERT_FINAL = "ᆨᆩᆪᆫᆬᆭᆮ ᆯᆰᆱᆲᆳᆴᆵᆶᆷᆸ ᆹᆺᆻᆼᆽ ᆾᆿᇀᇁᇂ"

    private const val COMPAT_JAMO = COMPAT_CONSONANT + COMPAT_VOWEL + COMPAT_CONSONANT
    private const val CONVERT_JAMO = CONVERT_INITIAL + CONVERT_MEDIAL + CONVERT_FINAL

}