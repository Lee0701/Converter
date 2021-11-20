package io.github.lee0701.converter.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.AttributeSet
import androidx.preference.Preference

class EnableOverlayPermissionsPreference(context: Context?, attrs: AttributeSet?)
    : Preference(context, attrs) {

    override fun onClick() {
        openOverlayPermissionSettings(context)
    }

    companion object {
        fun openOverlayPermissionSettings(context: Context) {
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                context.startActivity(intent)
            }
        }
    }
}