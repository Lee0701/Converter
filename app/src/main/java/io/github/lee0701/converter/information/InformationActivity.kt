package io.github.lee0701.converter.information

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.R
import io.github.lee0701.converter.databinding.ActivityInformationBinding

class InformationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setTitle(R.string.information_label)
    }

    companion object {
        fun agree(activity: Activity) {
            val editor = PreferenceManager.getDefaultSharedPreferences(activity).edit()
            editor.putBoolean("accessibility_service_agreed", true)
            editor.apply()
            // Open accessibility service settings
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            activity.startActivity(intent)
            activity.finish()
        }
    }
}