package com.heroconfigmanager.ui.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.heroconfigmanager.data.model.HERO_ROLES
import com.heroconfigmanager.databinding.FragmentConfigBinding
import com.heroconfigmanager.ui.SharedViewModel
import com.heroconfigmanager.ui.UiState

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = androidx.lifecycle.ViewModelProvider(
            (requireActivity().application as com.heroconfigmanager.HeroConfigApp).appViewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SharedViewModel::class.java]

        viewModel.config.observe(viewLifecycleOwner) { config ->
            binding.etConfigVersion.setText(config.configVersion)
            binding.etConfigNote.setText(config.configNote)
            buildStats(config.roles.marksman.size, config.roles.fighter.size,
                config.roles.assassin.size, config.roles.tank.size,
                config.roles.mage.size, config.roles.support.size)
        }

        viewModel.lastSync.observe(viewLifecycleOwner) { time ->
            binding.tvLastSync.text = if (time.isNotEmpty()) "Last synced: $time" else ""
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> binding.btnPushToGitHub.isEnabled = false
                is UiState.Success -> {
                    binding.btnPushToGitHub.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    viewModel.clearState()
                }
                is UiState.Error -> {
                    binding.btnPushToGitHub.isEnabled = true
                    Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.clearState()
                }
                else -> binding.btnPushToGitHub.isEnabled = true
            }
        }

        binding.btnPushToGitHub.setOnClickListener {
            val version = binding.etConfigVersion.text.toString().trim()
            val note    = binding.etConfigNote.text.toString().trim()
            if (version.isBlank()) {
                binding.etConfigVersion.error = "Version is required"
                return@setOnClickListener
            }
            viewModel.updateConfigMetadata(version, note)
        }
    }

    private fun buildStats(vararg counts: Int) {
        binding.statsContainer.removeAllViews()
        val total = counts.sum().coerceAtLeast(1)
        HERO_ROLES.forEachIndexed { i, role ->
            val count = counts[i]
            val row = LayoutInflater.from(requireContext())
                .inflate(android.R.layout.simple_list_item_2, binding.statsContainer, false)
            val label = row.findViewById<android.widget.TextView>(android.R.id.text1)
            val detail = row.findViewById<android.widget.TextView>(android.R.id.text2)
            label.text  = role
            detail.text = "$count heroes"
            binding.statsContainer.addView(row)

            val bar = LinearProgressIndicator(requireContext()).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 12 }
                max = total
                progress = count
                isIndeterminate = false
            }
            binding.statsContainer.addView(bar)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
