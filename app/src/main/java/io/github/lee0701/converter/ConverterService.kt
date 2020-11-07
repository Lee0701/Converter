package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.view.accessibility.AccessibilityEvent
import kotlinx.android.synthetic.main.candidates_view.view.*
import android.view.WindowManager.LayoutParams.*
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.lee0701.converter.dictionary.MapDictionary
import io.github.lee0701.converter.dictionary.PrefixSearchDictionary

class ConverterService: AccessibilityService() {

    private val rect = Rect()
    private val statusBarHeight get() = resources.getDimensionPixelSize(
        resources.getIdentifier("status_bar_height", "dimen", "android"))

    lateinit var dictionary: MapHanjaDictionary
    private var candidatesView: View? = null
    private var conversionIndex = 0
    private var preserveConversionIndex = false

    override fun onCreate() {
        super.onCreate()
        val map = mutableMapOf<String, List<HanjaDictionary.Entry>>()
        val br = assets.open("wordlist_example.txt").bufferedReader()
        while(true) {
            val line = br.readLine() ?: break
            val values = line.split(",")
            if(values.size < 2) continue
            val key = values[1]
            val value = values[0]
            val extra = values.getOrNull(2)
            val frequency = values.getOrNull(3)?.toInt() ?: 0
            val entry = HanjaDictionary.Entry(value, extra, frequency)
            map[key] = (map[key] ?: listOf()) + entry
        }
        dictionary = MapHanjaDictionary(map)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if(event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        val source = event.source
        source.getBoundsInScreen(rect)
        val text = event.text.firstOrNull() ?: return
        val word = text.split("\\s".toRegex()).lastOrNull()
        if(!preserveConversionIndex)conversionIndex = 0
        preserveConversionIndex = false
        if(word != null) onWord(word) { replaceWord(source, text.toString(), word, it) }
    }

    private fun replaceWord(source: AccessibilityNodeInfo, fullText: String, original: String, replacement: String) {
        val fullReplacement = replacement + original.drop(replacement.length)
        val pasteText = fullText.dropLast(original.length) + fullReplacement
        pasteFullText(source, pasteText)
        if(fullReplacement == original) {
            onWord(fullReplacement) { replaceWord(source, fullText, fullReplacement, it) }
        } else {
            preserveConversionIndex = true
        }
    }

    private fun pasteFullText(source: AccessibilityNodeInfo, text: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun onWord(word: String, onReplacement: (String) -> Unit) {
        val conversionTarget = word.drop(conversionIndex)
        if(conversionTarget.isEmpty()) return destroyWindow()

        val result = dictionary.search(conversionTarget)
        if(result == null) destroyWindow()
        else {
            val original = listOf(conversionTarget[0].toString(), conversionTarget).toHashSet().toList()
            val candidates = original + result.flatten().map { it.result }
            showWindow(candidates) {
                val replacement = word.take(conversionIndex) + it
                conversionIndex += it.length
                onReplacement(replacement)
            }
        }
    }

    private fun showWindow(candidates: List<String>, onReplacement: (String) -> Unit) {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if(candidatesView == null) {
            candidatesView = LayoutInflater.from(this).inflate(R.layout.candidates_view, null)

            val height = 200
            val heightPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height.toFloat(), resources.displayMetrics).toInt()
            val y = if(rect.centerY() < resources.displayMetrics.heightPixels / 2) rect.bottom else rect.top - heightPixels

            val type = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) TYPE_APPLICATION_OVERLAY else TYPE_SYSTEM_ALERT
            val flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL

            val params = WindowManager.LayoutParams(
                WRAP_CONTENT, heightPixels,
                rect.left, y - statusBarHeight,
                type, flags,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.LEFT
            windowManager.addView(candidatesView, params)
        }
        val view = candidatesView ?: return
        view.list.layoutManager = LinearLayoutManager(this)
        view.list.adapter = CandidateListAdapter(candidates.toTypedArray(), onReplacement)
    }

    private fun destroyWindow() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if(candidatesView != null) windowManager.removeView(candidatesView)
        candidatesView = null
    }

    override fun onInterrupt() {
    }
    
    class MapHanjaDictionary(entries: Map<String, List<HanjaDictionary.Entry>>)
        : PrefixSearchDictionary<List<HanjaDictionary.Entry>>(MapDictionary(entries)) {
        override fun search(key: String): List<List<HanjaDictionary.Entry>>? {
            return super.search(key)?.map { list -> list.sortedByDescending { it.frequency } }
        }
    }

    class CandidateListAdapter(private val dataset: Array<String>, private val onItemClick: (String) -> Unit)
        : RecyclerView.Adapter<CandidateListAdapter.CandidateItemViewHolder>() {

        class CandidateItemViewHolder(val textView: TextView): RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateItemViewHolder {
            val textView = LayoutInflater.from(parent.context).inflate(R.layout.candidate_item, parent, false) as TextView
            return CandidateItemViewHolder(textView)
        }

        override fun onBindViewHolder(holder: CandidateItemViewHolder, position: Int) {
            holder.textView.text = dataset[position]
            holder.textView.setOnClickListener { this.onItemClick(dataset[position]) }
        }

        override fun getItemCount(): Int {
            return dataset.size
        }
    }

}