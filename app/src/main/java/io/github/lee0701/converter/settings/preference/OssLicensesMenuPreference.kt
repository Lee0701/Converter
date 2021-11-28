package io.github.lee0701.converter.settings.preference

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.preference.Preference
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

class OssLicensesMenuPreference(context: Context, attrs: AttributeSet): Preference(context, attrs) {
    override fun onClick() {
        context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        super.onClick()
    }
}