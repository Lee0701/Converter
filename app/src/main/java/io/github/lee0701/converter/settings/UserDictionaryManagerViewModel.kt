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

class UserDictionaryManagerViewModel(application: Application) : AndroidViewModel(application) {

    val database = Room.databaseBuilder(application,
        UserDictionaryDatabase::class.java, ConverterService.DB_USER_DICTIONARY).build()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _dictionaries = MutableLiveData<List<UserDictionary>>()
    val dictionaries: LiveData<List<UserDictionary>> = _dictionaries

    private val _selectedDictionary = MutableLiveData<UserDictionary>()
    val selectedDictionary: LiveData<UserDictionary> = _selectedDictionary

    private val _words = MutableLiveData<List<UserDictionaryWord>>()
    val words: LiveData<List<UserDictionaryWord>> = _words

    fun loadAllDictionaries() {
        coroutineScope.launch {
            val list = database.dictionaryDao().getAllDictionaries().toList()
            viewModelScope.launch { _dictionaries.value = list }
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

}