package io.github.lee0701.converter.information

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.R
import io.github.lee0701.converter.databinding.InformationFragmentHanjaMixedBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class InformationFragment : Fragment() {

    private var _binding: InformationFragmentHanjaMixedBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InformationFragmentHanjaMixedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val locale = ConfigurationCompat.getLocales(resources.configuration)[0]

        if(locale.language != "ko") binding.viewHangulOnly.visibility = View.INVISIBLE
        binding.viewHangulOnly.setOnCheckedChangeListener { compoundButton, checked ->
            if(checked) {
                activity?.setTitle(R.string.information_label_hangul_only)
                binding.textviewFirst.setText(R.string.information_details_hangul_only)
                binding.textviewSecond.setText(R.string.information_question_hangul_only)
                binding.buttonAgree.setText(R.string.agree_hangul_only)
                binding.buttonDisagree.setText(R.string.disagree_hangul_only)
            } else {
                activity?.setTitle(R.string.information_label)
                binding.textviewFirst.setText(R.string.information_details)
                binding.textviewSecond.setText(R.string.information_question)
                binding.buttonAgree.setText(R.string.agree)
                binding.buttonDisagree.setText(R.string.disagree)
            }
        }

        binding.buttonAgree.setOnClickListener {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putBoolean("accessibility_service_agreed", true)
            editor.apply()
            // Open accessibility service settings
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            startActivity(intent)
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