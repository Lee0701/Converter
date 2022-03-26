package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import androidx.room.Room
import io.github.lee0701.converter.CharacterSet.isHangul
import io.github.lee0701.converter.candidates.Candidate
import io.github.lee0701.converter.assistant.HorizontalInputAssistantLauncherWindow
import io.github.lee0701.converter.assistant.InputAssistantLauncherWindow
import io.github.lee0701.converter.assistant.VerticalInputAssistantLauncherWindow
import io.github.lee0701.converter.assistant.InputAssistantWindow
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
import kotlin.math.max
import kotlin.math.min

class ConverterAccessibilityService: AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private lateinit var converter: HanjaConverter
    private var predictor: Predictor? = null

    private lateinit var candidatesWindow: CandidatesWindow
    private lateinit var inputAssistantWindow: InputAssistantWindow
    private lateinit var inputAssistantLauncherWindow: InputAssistantLauncherWindow

    // Accessibility Node where text from input assistant is pasted to
    private var source: AccessibilityNodeInfo? = null

    private var composingText = ComposingText("", 0)

    // Preference vars
    private var outputFormat: OutputFormat? = null
    private var enableAutoHiding = false
    private var assistantEnabledApps: Set<String> = setOf()

    private val rect = Rect()
    private var ignoreText: CharSequence? = null

    override fun onCreate() {
        super.onCreate()
        if(!getSharedPreferences(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, Context.MODE_PRIVATE)
                .getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
            SettingsActivity.PREFERENCE_LIST.forEach {
                PreferenceManager.setDefaultValues(this, it, true)
            }
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

    fun restartService() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this@ConverterAccessibilityService)

        outputFormat = preferences.getString("output_format", "hanja_only")?.let { OutputFormat.of(it) }
        enableAutoHiding = preferences.getBoolean("enable_auto_hiding", false)
        assistantEnabledApps = preferences.getStringSet("assistant_enabled_apps", setOf()) ?: setOf()

        val sortByContext = preferences.getBoolean("sort_by_context", false)
        val usePrediction = preferences.getBoolean("use_prediction", false)
        val autoComplete = preferences.getBoolean("use_autocomplete", false)

        val tfLitePredictor = if(BuildConfig.IS_DONATION && (usePrediction || sortByContext)) {
                TFLitePredictor(
                    assets.open("ml/wordlist.txt"),
                    assets.openFd("ml/model.tflite"),
                )
        } else null

        val converters = mutableListOf<HanjaConverter>()

        // Add Converter with User Dictionary
        val userDictionaryDatabase = Room.databaseBuilder(applicationContext, UserDictionaryDatabase::class.java, DB_USER_DICTIONARY).build()
        val userDictionaryHanjaConverter = DictionaryHanjaConverter(UserDictionaryDictionary(userDictionaryDatabase))
        converters += userDictionaryHanjaConverter

        // Add Converter with User Input History
        if(BuildConfig.IS_DONATION && preferences.getBoolean("use_learned_word", false)) {
            val historyDatabase = Room.databaseBuilder(applicationContext, HistoryDatabase::class.java, DB_HISTORY).build()
            val historyHanjaConverter = HistoryHanjaConverter(historyDatabase, preferences.getBoolean("freeze_learning", false))
            CoroutineScope(Dispatchers.IO).launch { historyHanjaConverter.deleteOldWords() }
            converters += historyHanjaConverter
        }

        // Add Converter with Main Compound Dictionary
        val additional = preferences.getStringSet("additional_dictionaries", setOf())?.toList() ?: listOf()
        val dictionaries = DictionaryManager.loadCompoundDictionary(assets, listOf("base") + additional)
        val dictionaryHanjaConverter: HanjaConverter = DictionaryHanjaConverter(dictionaries)

        if(tfLitePredictor != null && sortByContext) {
            converters += ContextSortingHanjaConverter(dictionaryHanjaConverter, CachingTFLitePredictor(tfLitePredictor))
        } else {
            converters += dictionaryHanjaConverter
        }

        // Add Converters with Specialized Dictionaries
        if(BuildConfig.IS_DONATION && preferences.getBoolean("search_by_translation", false)) {
            val dictionary = DictionaryManager.loadDictionary(assets, "translation")
            val color = ResourcesCompat.getColor(resources, R.color.searched_by_translation, theme)
            if(dictionary != null) converters += SpecializedHanjaConverter(dictionary, color)
        }
        if(BuildConfig.IS_DONATION && preferences.getBoolean("search_by_composition", false)) {
            val dictionary = DictionaryManager.loadDictionary(assets, "composition")
            val color = ResourcesCompat.getColor(resources, R.color.searched_by_composition, theme)
            if(dictionary != null) converters += SpecializedHanjaConverter(dictionary, color)
        }

        val hanjaConverter: HanjaConverter = CompoundHanjaConverter(converters.toList())

        converter = hanjaConverter
        if(usePrediction && autoComplete && tfLitePredictor != null) {
            predictor = ResortingPredictor(
                DictionaryPredictor(dictionaries),
                CachingTFLitePredictor(tfLitePredictor),
            )
        } else if(usePrediction && !autoComplete && tfLitePredictor != null) {
            predictor = NextWordPredictor(CachingTFLitePredictor(tfLitePredictor))
        } else if(!usePrediction && autoComplete) {
            predictor = DictionaryPredictor(dictionaries)
        } else {
            predictor = null
        }

        candidatesWindow = when(preferences.getString("window_type", "horizontal")) {
            "horizontal" -> HorizontalCandidatesWindow(this@ConverterAccessibilityService)
            else -> VerticalCandidatesWindow(this@ConverterAccessibilityService)
        }

        inputAssistantWindow = InputAssistantWindow(this)
        inputAssistantLauncherWindow = when(preferences.getString("window_type", "horizontal")) {
            "horizontal" -> HorizontalInputAssistantLauncherWindow(this)
            else -> VerticalInputAssistantLauncherWindow(this)
        }

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(event == null) return

        when(event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if(event.source != null && !isEditText(event.source.className)) {
                    inputAssistantLauncherWindow.hide()
                    if(enableAutoHiding) candidatesWindow.destroy()
                }
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if(event.source != null && !isEditText(event.source.className)) {
                    inputAssistantLauncherWindow.hide()
                    if(enableAutoHiding) candidatesWindow.destroy()
                }
                val isHideEvent = CandidatesWindowHider.of(event.packageName?.toString() ?: "")?.isHideEvent(event)
                if(enableAutoHiding && isHideEvent == true) {
                    candidatesWindow.destroy()
                    inputAssistantLauncherWindow.hide()
                }
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                // Update paste target.
                // Prevent from input assistant window itself being targeted as paste target
                if(event.packageName != this.packageName
                    && event.source != null && isEditText(event.source.className)) {
                        this.source = event.source
                }
            }
            else -> {}
        }

        val inputAssistantMode = event.packageName in assistantEnabledApps

        if(inputAssistantMode) onInputAssistant(event)
        else onNormalConversion(event)
    }

    private fun onInputAssistant(event: AccessibilityEvent) {
        when(event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                if(event.source != null && isEditText(event.source.className)) {
                    showInputAssistantLauncherWindow(event.source)
                } else {
                    inputAssistantLauncherWindow.hide()
                }
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                if(event.source != null && isEditText(event.source.className)) {
                    showInputAssistantLauncherWindow(event.source)
                }
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                if(event.source != null && isEditText(event.source.className)) {
                    val rect = Rect().apply { event.source.getBoundsInScreen(this) }
                    inputAssistantLauncherWindow.apply {
                        if(this is VerticalInputAssistantLauncherWindow) {
                            xPos = rect.left
                            yPos = rect.top
                        }
                    }.updateLayout()
                }
            }
            else -> {}
        }
    }

    private fun onNormalConversion(event: AccessibilityEvent) {
        when(event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val source = event.source ?: return
                source.getBoundsInScreen(rect)

                val ignoreText = this.ignoreText
                this.ignoreText = null
                val text = event.text.firstOrNull() ?: ""
                if(text == ignoreText) return

                val beforeText = event.beforeText ?: ""
                val fromIndex = event.fromIndex.let { if(it == -1) firstDifference(beforeText, text) else it }
                val addedCount = max(event.addedCount, 0)
                val removedCount = max(event.removedCount, 0)

                val newComposingText = composingText.textChanged(text, fromIndex, addedCount, removedCount)
                if(newComposingText.from != composingText.from) learnPreviouslyConverted()
                composingText = newComposingText

                if(addedCount == 0 && removedCount > 0) {
                    // Do delayed conversion when character is only deleted (backspace)
                    convert(source, 500)
                } else {
                    convert(source)
                }

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

    private fun convert(source: AccessibilityNodeInfo, delay: Long = 0) {
        job?.cancel()
        job = scope.launch {
            if(delay > 0) delay(delay)
            if(!isActive) return@launch
            val predictor = predictor
            val converted = converter.convertPrefix(composingText).flatten()
            val predicted = predictor?.predict(composingText)?.top(10) ?: listOf()
            val candidates = getExtraCandidates(composingText.composing) +
                    (if(converted.isNotEmpty()) predicted.take(1) else predicted) +
                    converted
            if(candidates.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    candidatesWindow.show(candidates, rect) { candidate ->
                        val replaced = composingText.replaced(candidate, outputFormat)
                        ignoreText = replaced.text
                        pasteFullText(source, replaced.text)
                        setSelection(source, replaced.to)
                        composingText = replaced
                        convert(source)
                        if(candidate.learnable) {
                            learn(candidate.hangul, candidate.hanja)
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    candidatesWindow.destroy()
                }
            }
        }
    }

    private fun showInputAssistantLauncherWindow(source: AccessibilityNodeInfo) {
        if(inputAssistantLauncherWindow.shown) return
        val rect = Rect().apply { source.getBoundsInScreen(this) }
        inputAssistantLauncherWindow.apply {
            if(this is VerticalInputAssistantLauncherWindow) {
                xPos = rect.left
                yPos = rect.top
            }
        }.show {
            inputAssistantLauncherWindow.hide()
            inputAssistantWindow.show { text ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(text, text)
                inputAssistantWindow.hide()
                candidatesWindow.destroy()
                clipboard.setPrimaryClip(clip)
                val postClose = {
                    pasteClipboard()
                }
                handler.postDelayed(postClose, 300)
            }
        }
    }

    fun closeCandidatesWindow() {
        candidatesWindow.destroy()
    }

    fun closeInputAssistantWindow() {
        inputAssistantWindow.hide()
        inputAssistantLauncherWindow.hide()
    }

    private fun isEditText(className: CharSequence): Boolean {
        return className == "android.widget.EditText"
    }

    // Does not take AccessibilityNodeInfo as parameter because the node info before launching input assistant should be used
    fun pasteClipboard() {
        val source = this.source ?: return
        source.performAction(AccessibilityNodeInfo.ACTION_PASTE)
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

    private fun learnPreviouslyConverted() {
        if(composingText.selected.isNotEmpty() && composingText.selected.all { it.learnable }) {
            learn(composingText.unconverted, composingText.converted)
        }
    }

    private fun learn(input: String, result: String) {
        val converter = converter
        if(converter !is LearningHanjaConverter) return
        CoroutineScope(Dispatchers.IO).launch { converter.learn(input, result) }
    }

    private fun getExtraCandidates(hangul: CharSequence): List<Candidate> {
        if(hangul.isEmpty()) return emptyList()
        val list = mutableListOf<CharSequence>()
        val nonHangulIndex = hangul.indexOfFirst { c -> !CharacterSet.isHangul(c) }
        list += if(nonHangulIndex > 0) hangul.slice(0 until nonHangulIndex) else hangul
        if(CharacterSet.isHangul(hangul[0])) list.add(0, hangul[0].toString())
        return list.map { Candidate(it.toString(), it.toString(), "") }
    }

    private fun firstDifference(a: CharSequence, b: CharSequence): Int {
        val len = min(a.length, b.length)
        for(i in 0 until len) {
            if(a[i] != b[i]) return i
        }
        return len
    }

    companion object {
        var INSTANCE: ConverterAccessibilityService? = null

        const val DB_HISTORY = "history"
        const val DB_USER_DICTIONARY = "user_dictionary"
    }

}