package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.*
import android.view.WindowManager.LayoutParams.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.lee0701.converter.dictionary.DiskDictionary
import io.github.lee0701.converter.dictionary.ListDictionary
import io.github.lee0701.converter.dictionary.PrefixSearchDictionary
import kotlinx.android.synthetic.main.candidate_item.view.*
import kotlinx.android.synthetic.main.candidates_view.view.*

class ConverterService: AccessibilityService() {

    private val handler = Handler()

    private val rect = Rect()
    private val statusBarHeight get() = resources.getDimensionPixelSize(
        resources.getIdentifier("status_bar_height", "dimen", "android"))

    private lateinit var source: AccessibilityNodeInfo
    private lateinit var dictionary: PrefixSearchHanjaDictionary
    private var candidatesView: View? = null
    private var conversionIndex = 0
    private var preserveConversionIndex = false

    override fun onCreate() {
        super.onCreate()
        dictionary = PrefixSearchHanjaDictionary(DiskDictionary(assets.open("dict.bin")))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when(event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                val source = event.source ?: return
                source.getBoundsInScreen(rect)
                val text = event.text.firstOrNull()?.toString() ?: return destroyWindow()
                val word = text.split("\\s".toRegex()).lastOrNull()
                if(!preserveConversionIndex) conversionIndex = 0
                preserveConversionIndex = false
                if(word != null) {
                    fun replace(it: String) {
                        val replacement = it + word.drop(it.length)
                        val pasteText = text.dropLast(word.length) + replacement
                        pasteFullText(source, pasteText)
                        preserveConversionIndex = true
                        handler.post { onWord(replacement) { replace(it) } }
                    }
                    handler.removeCallbacksAndMessages(null)
                    onWord(word) { replace(it) }
                }
            }
            else -> {}
        }
    }

    private fun pasteFullText(source: AccessibilityNodeInfo, text: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun onWord(word: String, onReplacement: (String) -> Unit) {
        val conversionTarget = preProcessConversionTarget(word.drop(conversionIndex))
        conversionIndex += (word.length - conversionIndex) - conversionTarget.length
        if(conversionTarget.isEmpty()) return destroyWindow()

        val result = dictionary.search(conversionTarget)
        if(result == null) destroyWindow()
        else {
            val original = getExtraCandidates(conversionTarget).toHashSet().toList().map { Candidate(it, "") }
            val candidates = original + result.flatten().map { Candidate(it.result, it.extra ?: "") }
            showWindow(candidates) {
                val replacement = word.take(conversionIndex) + it
                conversionIndex += it.length
                onReplacement(replacement)
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showWindow(candidates: List<Candidate>, onReplacement: (String) -> Unit) {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if(candidatesView == null) {
            val candidatesView = LayoutInflater.from(this).inflate(R.layout.candidates_view, null)
            candidatesView.close.setOnClickListener { destroyWindow() }
            candidatesView.list.layoutManager = LinearLayoutManager(this)
            candidatesView.list.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val p = (candidatesView.list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    val count = candidatesView.list.adapter?.itemCount ?: 0
                    candidatesView.count.text = resources.getString(R.string.candidates_count_format).format(p, count)
                }
            })

            val width = 160
            val widthPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width.toFloat(), resources.displayMetrics).toInt()

            val height = 160
            val heightPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height.toFloat(), resources.displayMetrics).toInt()
            val y = if(rect.centerY() < resources.displayMetrics.heightPixels / 2) rect.bottom else rect.top - heightPixels

            val type = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) TYPE_APPLICATION_OVERLAY else TYPE_SYSTEM_ALERT
            val flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL

            val params = WindowManager.LayoutParams(
                widthPixels, heightPixels,
                rect.left, y - statusBarHeight,
                type, flags,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            windowManager.addView(candidatesView, params)
            this.candidatesView = candidatesView
        }
        val view = candidatesView ?: return
        view.list.adapter = CandidateListAdapter(candidates.toTypedArray(), onReplacement)
        view.list.scrollToPosition(0)
    }

    private fun destroyWindow() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if(candidatesView != null) windowManager.removeView(candidatesView)
        candidatesView = null
    }

    override fun onInterrupt() {
    }

    private fun preProcessConversionTarget(conversionTarget: String): String {
        if(conversionTarget.isEmpty()) return conversionTarget
        if(!isHanja(conversionTarget[0])) return conversionTarget
        val hanjaIndex = conversionTarget.indexOfLast { c -> isHanja(c) }
        if(hanjaIndex == -1 || hanjaIndex == conversionTarget.length-1) return conversionTarget
        else return conversionTarget.substring(hanjaIndex + 1)
    }

    private fun getExtraCandidates(conversionTarget: String): List<String> {
        val list = mutableListOf<String>()
        val hangulIndex = conversionTarget.indexOfFirst { c -> isHangul(c) }
        if(hangulIndex == -1) list += conversionTarget
        else if(hangulIndex > 0) list += conversionTarget.substring(0 until hangulIndex)
        if(isHangul(conversionTarget[0])) list += conversionTarget[0].toString()
        return list.toList()
    }

    private fun isHanja(c: Char) = c.toInt() in 0x4E00 .. 0x62FF
            || c.toInt() in 0x6300 .. 0x77FF || c.toInt() in 0x7800 .. 0x8CFF
            || c.toInt() in 0x8D00 .. 0x9FFF || c.toInt() in 0x3400 .. 0x4DBF

    private fun isHangul(c: Char) = c.toInt() in 0xAC00 .. 0xD7AF
            || c.toInt() in 0x1100 .. 0x11FF || c.toInt() in 0xA960 .. 0xA97F
            || c.toInt() in 0xD7B0 .. 0xD7FF || c.toInt() in 0x3130 .. 0x318F

    class PrefixSearchHanjaDictionary(dictionary: ListDictionary<HanjaDictionary.Entry>)
        : PrefixSearchDictionary<List<HanjaDictionary.Entry>>(dictionary)

    class CandidateListAdapter(private val data: Array<Candidate>, private val onItemClick: (String) -> Unit)
        : RecyclerView.Adapter<CandidateListAdapter.CandidateItemViewHolder>() {

        class CandidateItemViewHolder(val view: View): RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateItemViewHolder {
            val textView = LayoutInflater.from(parent.context).inflate(R.layout.candidate_item, parent, false)
            return CandidateItemViewHolder(textView)
        }

        override fun onBindViewHolder(holder: CandidateItemViewHolder, position: Int) {
            val text = holder.view.text as TextView
            val extra = holder.view.extra as TextView
            text.text = data[position].text
            extra.text = data[position].extra
            holder.view.setOnClickListener { this.onItemClick(data[position].text) }
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }

    data class Candidate(
        val text: String,
        val extra: String
    )

}