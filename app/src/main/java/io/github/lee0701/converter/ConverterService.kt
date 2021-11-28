package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.ktx.assetsPath
import com.google.android.play.core.ktx.status
import com.google.android.play.core.tasks.Task
import io.github.lee0701.converter.CharacterSet.isHangul
import io.github.lee0701.converter.candidates.view.CandidatesWindow
import io.github.lee0701.converter.candidates.view.CandidatesWindowHider
import io.github.lee0701.converter.candidates.view.HorizontalCandidatesWindow
import io.github.lee0701.converter.candidates.view.VerticalCandidatesWindow
import io.github.lee0701.converter.dictionary.UserDictionaryDictionary
import io.github.lee0701.converter.engine.*
import io.github.lee0701.converter.history.HistoryDatabase
import io.github.lee0701.converter.settings.SettingsActivity
import io.github.lee0701.converter.userdictionary.UserDictionaryDatabase
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

class ConverterService: AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private lateinit var converter: Converter
    private var predictor: Predictor? = null
    private lateinit var candidatesWindow: CandidatesWindow

    private var composingText = ComposingText("", 0)

    private var outputFormat: OutputFormat? = null
    private val rect = Rect()
    private var ignoreText: CharSequence? = null

    private var enableAutoHiding = false

    override fun onCreate() {
        super.onCreate()
        SettingsActivity.PREFERENCE_LIST.forEach {
            PreferenceManager.setDefaultValues(this, it, false)
        }
        restartService()
        INSTANCE = this
    }

    override fun onDestroy() {
        super.onDestroy()
        INSTANCE = null
    }

    override fun onInterrupt() {
    }

    fun restartService() = scope.launch {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this@ConverterService)

        outputFormat = preferences.getString("output_format", "hanja_only")?.let { OutputFormat.of(it) }
        enableAutoHiding = preferences.getBoolean("enable_auto_hiding", false)

        outputFormat =
            preferences.getString("output_format", "hanja_only")?.let { OutputFormat.of(it) }
        val sortByContext = preferences.getBoolean("sort_by_context", false)
        val usePrediction = preferences.getBoolean("use_prediction", false)

        val tfLitePredictor = if(BuildConfig.IS_DONATION && (usePrediction || sortByContext)) {
            loadTFLitePredictorAsync("prediction").await()
        } else null

        val converters = mutableListOf<HanjaConverter>()


        val userDictionaryDatabase = Room.databaseBuilder(applicationContext, UserDictionaryDatabase::class.java, DB_USER_DICTIONARY).build()
        val userDictionaryHanjaConverter = DictionaryHanjaConverter(UserDictionaryDictionary(userDictionaryDatabase))
        converters += userDictionaryHanjaConverter

        if(BuildConfig.IS_DONATION && preferences.getBoolean("use_learned_word", false)) {
            val historyDatabase = Room.databaseBuilder(applicationContext, HistoryDatabase::class.java, DB_HISTORY).build()
            val historyHanjaConverter = HistoryHanjaConverter(historyDatabase, preferences.getBoolean("freeze_learning", false))
            CoroutineScope(Dispatchers.IO).launch { historyHanjaConverter.deleteOldWords() }
            converters += historyHanjaConverter
        }

        val additional = preferences.getStringSet("additional_dictionaries", setOf())?.toList() ?: listOf()
        val dictionaries = DictionaryManager.loadCompoundDictionary(assets, listOf("base") + additional)
        val dictionaryHanjaConverter = DictionaryHanjaConverter(dictionaries)

        if(tfLitePredictor != null && sortByContext) {
            converters += ContextSortingHanjaConverter(dictionaryHanjaConverter, tfLitePredictor)
        } else {
            converters += dictionaryHanjaConverter
        }

        converter = Converter(CompoundHanjaConverter(converters.toList()))
        if(tfLitePredictor != null && usePrediction) predictor = Predictor(tfLitePredictor)
        else predictor = null

        candidatesWindow = when(preferences.getString("window_type", "horizontal")) {
            "horizontal" -> HorizontalCandidatesWindow(this@ConverterService)
            else -> VerticalCandidatesWindow(this@ConverterService)
        }

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(event == null) return
        when(event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val isHideEvent = CandidatesWindowHider.of(event.packageName?.toString() ?: "")?.isHideEvent(event)
                if(enableAutoHiding && isHideEvent == true) {
                    candidatesWindow.destroy()
                }
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val source = event.source ?: return
                source.getBoundsInScreen(rect)

                val ignoreText = this.ignoreText
                this.ignoreText = null
                val text = event.text.firstOrNull() ?: ""
                if(text == ignoreText) return

                val beforeText = event.beforeText ?: ""
                val fromIndex = event.fromIndex.let { if(it == -1) firstDifference(beforeText, text) else it }
                val addedCount = event.addedCount
                val removedCount = event.removedCount

                val newComposingText = composingText.textChanged(text, fromIndex, addedCount, removedCount)
                if(newComposingText.from != composingText.from) learnConverted()
                composingText = newComposingText

                convert(source)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                val source = event.source ?: return
                val start = source.textSelectionStart
                val end = source.textSelectionEnd
                val newComposingText = composingText.textSelectionChanged(start, end)
                if(composingText != newComposingText) {
                    this.composingText = newComposingText
                    convert(source)
                }
            }
            else -> {}
        }
    }

    private fun convert(source: AccessibilityNodeInfo) {
        job?.cancel()
        job = scope.launch {
            if(composingText.composing.isNotEmpty()) {
                val candidates = converter.convertAsync(scope, composingText).await()
                if(candidates.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        candidatesWindow.show(candidates, rect) { hanja ->
                            val hangul = composingText.composing.take(hanja.length).toString()
                            val replaced = composingText.replaced(hanja, outputFormat)
                            ignoreText = replaced.text
                            pasteFullText(source, replaced.text)
                            setSelection(source, replaced.to)
                            composingText = replaced
                            convert(source)
                            if(hanja.all { CharacterSet.isHanja(it) }) learn(hangul, hanja)
                        }
                    }
                } else {
                    candidatesWindow.destroy()
                }
            } else if(predictor != null) {
                learnConverted()
                val anyHangul = composingText.textBeforeCursor.any { isHangul(it) }
                if(anyHangul) {
                    val candidates = predictor?.predictAsync(scope, composingText)?.await()
                    if(candidates != null) withContext(Dispatchers.Main) {
                        candidatesWindow.show(candidates, rect) { prediction ->
                            val inserted = composingText.inserted(prediction)
                            ignoreText = inserted.text
                            pasteFullText(source, inserted.text)
                            setSelection(source, inserted.to)
                            composingText = inserted
                            convert(source)
                        }
                    }
                } else {
                    candidatesWindow.destroy()
                }
            } else {
                candidatesWindow.destroy()
            }
        }
    }

    private fun pasteFullText(source: AccessibilityNodeInfo, fullText: CharSequence) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, fullText)
        source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun setSelection(source: AccessibilityNodeInfo, start: Int, end: Int = start) {
        val arguments = Bundle()
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start)
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end)
        source.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments)
    }

    private fun learnConverted() {
        if(composingText.converted.isNotEmpty() && !composingText.converted.all { isHangul(it) }) {
            learn(composingText.unconverted, composingText.converted)
        }
    }

    private fun learn(input: String, result: String) {
        CoroutineScope(Dispatchers.IO).launch { converter.learn(input, result) }
    }

    private fun firstDifference(a: CharSequence, b: CharSequence): Int {
        val len = min(a.length, b.length)
        for(i in 0 until len) {
            if(a[i] != b[i]) return i
        }
        return len
    }

    private suspend fun loadTFLitePredictorAsync(assetPackName: String): Deferred<TFLitePredictor?> = scope.async {
        val assetPackManager = AssetPackManagerFactory.getInstance(applicationContext)
        val states = assetPackManager.getPackStates(listOf(assetPackName)).await()
        val state = states.result.packStates()?.get(assetPackName)
        when(state?.status) {
            AssetPackStatus.PENDING -> {
                delay(2000)
                return@async loadTFLitePredictorAsync(assetPackName).await()
            }
            AssetPackStatus.DOWNLOADING -> {
                CoroutineScope(Dispatchers.Main).launch {
                    val percent = 100.0 * state.bytesDownloaded() / state.totalBytesToDownload()
                    val text = resources.getString(R.string.asset_pack_download_progress).format(percent.toInt())
                    Toast.makeText(this@ConverterService, text, Toast.LENGTH_SHORT).show()
                }
                delay(2000)
                return@async loadTFLitePredictorAsync(assetPackName).await()
            }
            AssetPackStatus.TRANSFERRING -> {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@ConverterService, R.string.asset_pack_transferring, Toast.LENGTH_SHORT).show()
                }
                delay(2000)
                return@async loadTFLitePredictorAsync(assetPackName).await()
            }
            AssetPackStatus.COMPLETED -> {
                val assetPackPath = assetPackManager.packLocations[assetPackName]
                if(assetPackPath != null) {
                    val wordListPath = File(assetPackPath.assetsPath, "wordlist.txt").path
                    val modelPath = File(assetPackPath.assetsPath, "model.tflite").path
                    return@async TFLitePredictor(this@ConverterService, FileInputStream(wordListPath), FileInputStream(modelPath))
                }
            }
            AssetPackStatus.NOT_INSTALLED -> {
                assetPackManager.fetch(listOf(assetPackName)).await()
                return@async loadTFLitePredictorAsync(assetPackName).await()
            }
            AssetPackStatus.WAITING_FOR_WIFI -> {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@ConverterService, R.string.waiting_for_wifi, Toast.LENGTH_SHORT).show()
                }
                return@async null
            }
            else -> {

            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@ConverterService, R.string.asset_pack_load_failed, Toast.LENGTH_LONG).show()
        }
        return@async null
    }

    private suspend fun <T> Task<T>.await(): Task<T> = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if(cont.isActive) cont.resume(task)
        }
        addOnFailureListener { exception ->
            if(cont.isActive) cont.resumeWithException(exception)
        }
    }

    companion object {
        var INSTANCE: ConverterService? = null

        const val DB_HISTORY = "history"
        const val DB_USER_DICTIONARY = "user_dictionary"
    }

}