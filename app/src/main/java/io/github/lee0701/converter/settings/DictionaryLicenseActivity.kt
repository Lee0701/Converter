package io.github.lee0701.converter.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.lee0701.converter.databinding.ActivityDictionaryLicenseBinding

class DictionaryLicenseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDictionaryLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bytes = assets.open("dict.bin").readBytes()
        val endIndex = bytes.indexOf(0)
        val license = String(bytes.sliceArray(0 until endIndex))

        binding.text.text = license
    }
}