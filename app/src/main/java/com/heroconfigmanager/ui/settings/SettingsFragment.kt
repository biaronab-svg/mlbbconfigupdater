package com.heroconfigmanager.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.heroconfigmanager.data.repository.AppSettingsRepository
import com.heroconfigmanager.data.repository.ConfigRepository
import com.heroconfigmanager.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindSettings()

        binding.btnSaveToken.setOnClickListener {
            AppSettingsRepository.saveGitHubToken(
                binding.etGitHubToken.text?.toString().orEmpty()
            )
            ConfigRepository.clearCachedState()
            bindSettings()
            Toast.makeText(requireContext(), com.heroconfigmanager.R.string.token_saved, Toast.LENGTH_SHORT).show()
        }

        binding.btnResetToken.setOnClickListener {
            AppSettingsRepository.resetGitHubToken()
            ConfigRepository.clearCachedState()
            bindSettings()
            Toast.makeText(requireContext(), com.heroconfigmanager.R.string.token_reset, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindSettings() {
        binding.etGitHubToken.setText(AppSettingsRepository.getCustomGitHubToken())
        binding.chipTokenMode.text = getString(
            if (AppSettingsRepository.isUsingCustomGitHubToken()) {
                com.heroconfigmanager.R.string.token_mode_custom
            } else {
                com.heroconfigmanager.R.string.token_mode_default
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
