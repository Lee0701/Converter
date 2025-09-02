package io.github.lee0701.converter.settings.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference

class MultiSelectAppListPreference(
    context: Context,
    attributeSet: AttributeSet,
): MultiSelectListPreference(context, attributeSet) {

    init {
        val packages = context.packageManager?.getInstalledPackages(0)
        if(packages != null) {
            val list = packages.map { it.applicationInfo?.loadLabel(context.packageManager) to it.packageName }.sortedBy { it.first.toString()    }
            entries = list.map { it.first }.toTypedArray()
            entryValues = list.map { it.second }.toTypedArray()
        }
    }

}