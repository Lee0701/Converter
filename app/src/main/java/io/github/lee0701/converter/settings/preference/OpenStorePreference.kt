package io.github.lee0701.converter.settings.preference

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import androidx.preference.Preference

class OpenStorePreference(
    context: Context,
    attributeSet: AttributeSet
): Preference(context, attributeSet) {

    private val uri: String = attributeSet.getAttributeValue(null, "uri")

    override fun onClick() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uri)
            setPackage("com.android.vending")
        }
        context.startActivity(intent)
        super.onClick()
    }

}