package io.github.lee0701.converter

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.dictionary.DiskDictionary
import io.github.lee0701.converter.dictionary.ListDictionary
import io.github.lee0701.converter.dictionary.PrefixSearchDictionary

class HanjaConverter(private val context: Context, private val listener: Listener) {

    private val resources = context.resources
    private val statusBarHeight get() = resources.getDimensionPixelSize(
        resources.getIdentifier("status_bar_height", "dimen", "android"))

    private val windowSizeMultiplier = 40
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val columnCount = preferences.getInt("column_count", 2)
    private val windowWidth = getWindowSize(preferences.getInt("window_width", 5))
    private val windowHeight = getWindowSize(preferences.getInt("window_height", 4))

    private val outputFormat = preferences.getString("output_format", "hanja_only")?.let { OutputFormat.of(it) }

    private val candidatesWindow = CandidatesWindow(context, columnCount, windowWidth, windowHeight)
    private val dictionary = PrefixSearchHanjaDictionary(DiskDictionary(context.assets.open("dict.bin")))
    val rect = Rect()

    fun onWord(word: String) {
        val conversionTarget = preProcessConversionTarget(word)
        val lengthDiff = word.length - conversionTarget.length
        if(conversionTarget.isEmpty()) return candidatesWindow.destroy()

        val result = dictionary.search(conversionTarget)
        if(result == null) candidatesWindow.destroy()
        else {
            val extra = getExtraCandidates(conversionTarget).map { CandidatesWindow.Candidate(it, "") }
            val candidates = extra + result.map { list -> list.sortedByDescending { it.frequency } }
                .flatten().distinct().map { CandidatesWindow.Candidate(it.result, it.extra ?: "") }

            val yPos =
                if(rect.centerY() < resources.displayMetrics.heightPixels / 2) rect.bottom - statusBarHeight
                else rect.top - windowHeight - statusBarHeight

            candidatesWindow.show(candidates, rect.left, yPos) { hanja ->
                val hangul = word.drop(lengthDiff).take(hanja.length)
                val formatted = (outputFormat?.let { it(hanja, hangul) } ?: hanja)
                listener.onReplacement(formatted, lengthDiff, hanja.length)
            }
        }
    }

    private fun preProcessConversionTarget(conversionTarget: String): String {
        if(conversionTarget.isEmpty()) return conversionTarget
        if(isHangul(conversionTarget[0])) return conversionTarget
        val hangulIndex = conversionTarget.indexOfFirst { c -> isHangul(c) }
        if(hangulIndex == -1 || hangulIndex >= conversionTarget.length) return ""
        else return conversionTarget.substring(hangulIndex)
    }

    private fun getExtraCandidates(conversionTarget: String): List<String> {
        val list = mutableListOf<String>()
        val nonHangulIndex = conversionTarget.indexOfFirst { c -> !isHangul(c) }
        if(nonHangulIndex > 0) list += conversionTarget.substring(0 until nonHangulIndex)
        else list += conversionTarget
        if(isHangul(conversionTarget[0])) list.add(0, conversionTarget[0].toString())
        return list.toList()
    }

    private fun isHanja(c: Char) = c.toInt() in 0x4E00 .. 0x62FF
            || c.toInt() in 0x6300 .. 0x77FF || c.toInt() in 0x7800 .. 0x8CFF
            || c.toInt() in 0x8D00 .. 0x9FFF || c.toInt() in 0x3400 .. 0x4DBF

    private fun isHangul(c: Char) = c.toInt() in 0xAC00 .. 0xD7AF
            || c.toInt() in 0x1100 .. 0x11FF || c.toInt() in 0xA960 .. 0xA97F
            || c.toInt() in 0xD7B0 .. 0xD7FF || c.toInt() in 0x3130 .. 0x318F

    private fun getWindowSize(sizeInPref: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        (sizeInPref * windowSizeMultiplier).toFloat(),
        context.resources.displayMetrics
    ).toInt()

    class PrefixSearchHanjaDictionary(dictionary: ListDictionary<HanjaDictionary.Entry>)
        : PrefixSearchDictionary<List<HanjaDictionary.Entry>>(dictionary)

    interface Listener {
        fun onReplacement(replacement: String, index: Int, length: Int)
    }

}