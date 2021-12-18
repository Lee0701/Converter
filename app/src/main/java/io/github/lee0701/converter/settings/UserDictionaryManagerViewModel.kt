package io.github.lee0701.converter.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import io.github.lee0701.converter.ConverterService
import io.github.lee0701.converter.userdictionary.UserDictionary
import io.github.lee0701.converter.userdictionary.UserDictionaryDatabase
import io.github.lee0701.converter.userdictionary.UserDictionaryWord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class UserDictionaryManagerViewModel(application: Application) : AndroidViewModel(application) {

    val database = Room.databaseBuilder(application,
        UserDictionaryDatabase::class.java, ConverterService.DB_USER_DICTIONARY).build()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _dictionaries = MutableLiveData<List<UserDictionary>>()
    val dictionaries: LiveData<List<UserDictionary>> = _dictionaries

    private val _selectedDictionary = MutableLiveData<UserDictionary?>()
    val selectedDictionary: LiveData<UserDictionary?> = _selectedDictionary

    private val _words = MutableLiveData<List<UserDictionaryWord>>()
    val words: LiveData<List<UserDictionaryWord>> = _words

    fun loadAllDictionaries() {
        coroutineScope.launch {
            val list = database.dictionaryDao().getAllDictionaries().toList()
            viewModelScope.launch {
                _dictionaries.value = list
                if(dictionaries.value?.contains(selectedDictionary.value) != true) {
                    _selectedDictionary.value = null
                }
            }
        }
    }

    fun selectDictionary(dictionary: UserDictionary) {
        _selectedDictionary.value = dictionary
    }

    fun loadAllWords() {
        coroutineScope.launch {
            val dictionary = selectedDictionary.value
            if(dictionary == null) {
                viewModelScope.launch { _words.value = emptyList() }
            } else {
                val list = database.wordDao().getAllWords(dictionary.id).toList()
                viewModelScope.launch { _words.value = list }
            }
        }
    }

    fun insertWord(word: UserDictionaryWord) {
        coroutineScope.launch {
            database.wordDao().insertWords(word)
            loadAllWords()
        }
    }

    fun updateWord(oldWord: UserDictionaryWord, newWord: UserDictionaryWord) {
        coroutineScope.launch {
            if(oldWord.hangul == newWord.hangul && oldWord.hanja == newWord.hanja) {
                database.wordDao().updateWords(newWord)
            } else {
                database.wordDao().deleteWords(oldWord)
                database.wordDao().insertWords(newWord)
            }
            loadAllWords()
        }
    }

    fun deleteWord(word: UserDictionaryWord) {
        coroutineScope.launch {
            database.wordDao().deleteWords(word)
            loadAllWords()
        }
    }

    fun insertDictionary(dictionary: UserDictionary) {
        coroutineScope.launch {
            database.dictionaryDao().insertDictionary(dictionary)
            loadAllDictionaries()
        }
    }

    fun updateDictionary(dictionary: UserDictionary) {
        coroutineScope.launch {
            database.dictionaryDao().updateDictionary(dictionary)
            loadAllDictionaries()
        }
    }

    fun deleteDictionary(dictionary: UserDictionary) {
        coroutineScope.launch {
            database.dictionaryDao().deleteDictionary(dictionary)
            loadAllDictionaries()
        }
    }

    fun clearDictionary(dictionary: UserDictionary) {
        coroutineScope.launch {
            val words = database.wordDao().getAllWords(dictionary.id)
            database.wordDao().deleteWords(*words)
            loadAllWords()
        }
    }

    fun importDictionary(dictionary: UserDictionary, inputStream: InputStream) {
        coroutineScope.launch {
            val lines = inputStream.bufferedReader().readLines()
            val words = lines
                .filter { !it.startsWith('#') }
                .map { it.split(':') }.filter { it.size == 3 }
                .map { (hangul, hanja, description) -> UserDictionaryWord(dictionary.id, hangul, hanja, description) }
                .toTypedArray()
            database.wordDao().insertWords(*words)
            loadAllWords()
        }
    }

    fun exportDictionary(dictionary: UserDictionary, outputStream: OutputStream) {
        coroutineScope.launch {
            val words = database.wordDao().getAllWords(dictionary.id)
            val lines = words.map { word -> "${word.hangul}:${word.hanja}:${word.description}" }
            outputStream.write(lines.joinToString("\n").toByteArray())
        }
    }

}