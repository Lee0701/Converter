package io.github.lee0701.converter.settings

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.room.Room
import io.github.lee0701.converter.ConverterService
import io.github.lee0701.converter.R
import io.github.lee0701.converter.history.HistoryDatabase
import kotlinx.coroutines.*

class ResetLearningPreference(context: Context?, attrs: AttributeSet?): Preference(context, attrs) {
    override fun onClick() {
        AlertDialog.Builder(context)
            .setMessage(R.string.confirm_reset_learning)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val database = Room.databaseBuilder(context, HistoryDatabase::class.java, ConverterService.DB_HISTORY).build()
                CoroutineScope(Dispatchers.IO).launch {
                    database.wordDao().deleteWords(*database.wordDao().getAllWords())
                    ConverterService.INSTANCE?.restartService()
                }
            }.setNegativeButton(android.R.string.cancel) { _, _ ->
            }.show()
        super.onClick()
    }
}