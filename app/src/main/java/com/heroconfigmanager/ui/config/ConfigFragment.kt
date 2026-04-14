package com.heroconfigmanager.ui.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.heroconfigmanager.data.model.HERO_ROLES
import com.heroconfigmanager.databinding.FragmentConfigBinding
import com.heroconfigmanager.ui.SharedViewModel
import com.heroconfigmanager.ui.UiState

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = androidx.lifecycle.ViewModelProvider(
            (requireActivity().application as com.heroconfigmanager.HeroConfigApp).appViewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SharedViewModel::class.java]

        // Populate fields and stats whenever the in-memory config changes
        viewModel.config.observe(viewLifecycleOwner) { config ->
            // Avoid overwriting text the user is currently editing
            if (binding.etConfigVersion.text.toString() != config.configVersion) {
                binding.etConfigVersion.setText(config.configVersion)
            }
            if (binding.etConfigNote.text.toString() != config.configNote) {
                binding.etConfigNote.setText(config.configNote)
            }
            buildStats(
                config.roles.marksman.size,
                config.roles.fighter.size,
                config.roles.assassin.size,
                config.roles.tank.size,
                config.roles.mage.size,
                config.roles.support.size
            )
        }

        // Show last-sync timestamp as a compact badge
        viewModel.lastSync.observe(viewLifecycleOwner) { time ->
            binding.tvLastSync.text    = if (time.isNotEmpty()) "⟳ $time" else ""
            binding.tvLastSync.visibility = if (time.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // React to loading / success / error states
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.btnPushToGitHub.isEnabled = false
                    binding.btnPushToGitHub.text = "Pushing…"
                }
                is UiState.Success -> {
                    binding.btnPushToGitHub.isEnabled = true
                    binding.btnPushToGitHub.setText(com.heroconfigmanager.R.string.push_to_github)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    viewModel.clearState()
                }
                is UiState.Error -> {
                    binding.btnPushToGitHub.isEnabled = true
                    binding.btnPushToGitHub.setText(com.heroconfigmanager.R.string.push_to_github)
                    Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.clearState()
                }
                else -> {
                    binding.btnPushToGitHub.isEnabled = true
                    binding.btnPushToGitHub.setText(com.heroconfigmanager.R.string.push_to_github)
                }
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

    /** Builds per-role stat rows with progress bars inside statsContainer. */
    private fun buildStats(vararg counts: Int) {
        binding.statsContainer.removeAllViews()
        val total = counts.sum().coerceAtLeast(1)

        // Role icon labels (emoji-free, plain text)
        val roleColors = intArrayOf(
            com.heroconfigmanager.R.color.primary,
            com.heroconfigmanager.R.color.primary,
            com.heroconfigmanager.R.color.primary,
            com.heroconfigmanager.R.color.primary,
            com.heroconfigmanager.R.color.primary,
            com.heroconfigmanager.R.color.primary,
        )

        HERO_ROLES.forEachIndexed { i, role ->
            val count = counts[i]

            // Row: role label + count
            val row = LayoutInflater.from(requireContext())
                .inflate(android.R.layout.simple_list_item_2, binding.statsContainer, false)
            val label  = row.findViewById<android.widget.TextView>(android.R.id.text1)
            val detail = row.findViewById<android.widget.TextView>(android.R.id.text2)
            label.text  = role
            label.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
            detail.text = "$count ${if (count == 1) "hero" else "heroes"}"
            detail.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            binding.statsContainer.addView(row)

            // Progress bar
            val bar = LinearProgressIndicator(requireContext()).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                    it.bottomMargin = (14 * resources.displayMetrics.density).toInt()
                }
                max          = total
                progress     = count
                isIndeterminate = false
                trackCornerRadius = (4 * resources.displayMetrics.density).toInt()
            }
            binding.statsContainer.addView(bar)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
