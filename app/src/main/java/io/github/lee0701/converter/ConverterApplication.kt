package io.github.lee0701.converter

import android.app.Application
import com.google.android.material.color.DynamicColors

class ConverterApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}