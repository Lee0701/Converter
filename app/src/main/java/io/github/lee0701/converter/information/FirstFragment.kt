package io.github.lee0701.converter.information

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.databinding.InformationFragmentFirstBinding
import io.github.lee0701.converter.settings.SettingsActivity

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: InformationFragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = InformationFragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonAgree.setOnClickListener {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putBoolean("accessibility_service_agreed", true)
            editor.apply()
            startActivity(Intent(context, SettingsActivity::class.java))
            activity?.finish()
        }
        binding.buttonDisagree.setOnClickListener {
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}