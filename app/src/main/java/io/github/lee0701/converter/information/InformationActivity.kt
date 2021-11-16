package io.github.lee0701.converter.information

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

}