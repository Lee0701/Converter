package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
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
import io.github.lee0701.converter.dictionary.DiskDictionary
import io.github.lee0701.converter.dictionary.ListDictionary
import io.github.lee0701.converter.dictionary.PrefixSearchDictionary
import kotlinx.android.synthetic.main.candidate_item.view.*

class ConverterService: AccessibilityService() {

    private val rect = Rect()
    private val statusBarHeight get() = resources.getDimensionPixelSize(
        resources.getIdentifier("status_bar_height", "dimen", "android"))

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
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                destroyWindow()
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                val source = event.source ?: return
                source.getBoundsInScreen(rect)
                val text = event.text.firstOrNull()?.toString() ?: return
                val word = text.split("\\s".toRegex()).lastOrNull()
                if(!preserveConversionIndex) conversionIndex = 0
                preserveConversionIndex = false
                if(word != null) onWord(word) {
                    val replacement = it + word.drop(it.length)
                    val pasteText = text.dropLast(word.length) + replacement
                    pasteFullText(source, pasteText)
                    preserveConversionIndex = true
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
        val conversionTarget = word.drop(conversionIndex)
        if(conversionTarget.isEmpty()) return destroyWindow()

        val result = dictionary.search(conversionTarget)
        if(result == null) destroyWindow()
        else {
            val original = listOf(conversionTarget[0].toString(), conversionTarget).toHashSet().toList().map { Candidate(it, "") }
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