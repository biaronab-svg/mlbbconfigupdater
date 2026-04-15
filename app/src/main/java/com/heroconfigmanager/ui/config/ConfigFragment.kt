package com.heroconfigmanager.ui.config

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.widget.TextView
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.heroconfigmanager.R
import com.heroconfigmanager.data.model.HERO_ROLES
import com.heroconfigmanager.databinding.FragmentConfigBinding
import com.heroconfigmanager.ui.SharedViewModel
import com.heroconfigmanager.ui.UiState

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedViewModel

    private val roleColors = intArrayOf(
        R.color.role_marksman,
        R.color.role_fighter,
        R.color.role_assassin,
        R.color.role_tank,
        R.color.role_mage,
        R.color.role_support,
    )

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

        setupChart(binding.statsChart)

        viewModel.config.observe(viewLifecycleOwner) { config ->
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

        viewModel.lastSync.observe(viewLifecycleOwner) { time ->
            binding.tvLastSync.text = if (time.isNotEmpty()) "Synced $time" else ""
            binding.tvLastSync.visibility = if (time.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.btnPushToGitHub.isEnabled = false
                    binding.btnPushToGitHub.text = "Pushing..."
                }
                is UiState.Success -> {
                    binding.btnPushToGitHub.isEnabled = true
                    binding.btnPushToGitHub.setText(R.string.push_to_github)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    viewModel.clearState()
                }
                is UiState.Error -> {
                    binding.btnPushToGitHub.isEnabled = true
                    binding.btnPushToGitHub.setText(R.string.push_to_github)
                    Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    viewModel.clearState()
                }
                else -> {
                    binding.btnPushToGitHub.isEnabled = true
                    binding.btnPushToGitHub.setText(R.string.push_to_github)
                }
            }
        }

        binding.btnPushToGitHub.setOnClickListener {
            val version = binding.etConfigVersion.text.toString().trim()
            val note = binding.etConfigNote.text.toString().trim()
            if (version.isBlank()) {
                binding.etConfigVersion.error = "Version is required"
                return@setOnClickListener
            }
            viewModel.updateConfigMetadata(version, note)
        }
    }

    private fun setupChart(chart: PieChart) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setUsePercentValues(false)
        chart.setDrawEntryLabels(false)
        chart.setDrawMarkers(false)
        chart.setTouchEnabled(false)
        chart.isRotationEnabled = false
        chart.holeRadius = 72f
        chart.transparentCircleRadius = 78f
        chart.setHoleColor(Color.TRANSPARENT)
        chart.setTransparentCircleColor(MaterialColors.getColor(chart, com.google.android.material.R.attr.colorPrimaryContainer))
        chart.setTransparentCircleAlpha(28)
        chart.minAngleForSlices = 12f
        chart.setNoDataText(getString(R.string.stats_summary_empty))
        chart.setNoDataTextColor(MaterialColors.getColor(chart, com.google.android.material.R.attr.colorOnSurfaceVariant))
        chart.setExtraOffsets(0f, 12f, 0f, 12f)
    }

    private fun buildStats(vararg counts: Int) {
        binding.statsContainer.removeAllViews()

        val total = counts.sum()
        val activeRoles = counts.count { it > 0 }
        binding.tvStatsSummary.text = if (total == 0) {
            getString(R.string.stats_summary_empty)
        } else {
            getString(R.string.stats_summary_format, total, activeRoles)
        }

        updateChart(counts, total)

        HERO_ROLES.forEachIndexed { index, role ->
            val count = counts[index]
            val row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_stat_row, binding.statsContainer, false)

            val title = row.findViewById<TextView>(R.id.tvRoleName)
            val countView = row.findViewById<TextView>(R.id.tvRoleCount)
            val progress = row.findViewById<LinearProgressIndicator>(R.id.progressRole)

            title.text = role
            countView.text = if (count == 1) {
                getString(R.string.hero_count_singular, count)
            } else {
                getString(R.string.hero_count_plural, count)
            }
            countView.setTextColor(ContextCompat.getColor(requireContext(), roleColors[index]))

            progress.max = total.coerceAtLeast(1)
            progress.progress = count
            progress.trackCornerRadius = resources.getDimensionPixelSize(R.dimen.stat_bar_corner)
            progress.trackColor = MaterialColors.getColor(progress, com.google.android.material.R.attr.colorSurfaceContainerHighest)
            progress.setIndicatorColor(ContextCompat.getColor(requireContext(), roleColors[index]))

            binding.statsContainer.addView(row)
        }
    }

    private fun updateChart(counts: IntArray, total: Int) {
        val chart = binding.statsChart
        if (total == 0) {
            chart.clear()
            chart.centerText = getChartCenterText(0)
            chart.invalidate()
            return
        }

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        counts.forEachIndexed { index, count ->
            if (count > 0) {
                entries += PieEntry(count.toFloat(), HERO_ROLES[index])
                colors += ContextCompat.getColor(requireContext(), roleColors[index])
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 5f
            selectionShift = 0f
            setDrawValues(false)
        }

        chart.data = PieData(dataSet)
        chart.centerText = getChartCenterText(total)
        chart.highlightValues(null)
        chart.animateY(1400, Easing.EaseInOutCubic)
        chart.animateX(950, Easing.EaseOutQuart)
        chart.invalidate()
    }

    private fun getChartCenterText(total: Int): SpannableString {
        val title = getString(R.string.stats_center_title)
        val value = total.toString()
        val text = "$title\n$value"
        val titleColor = MaterialColors.getColor(binding.statsChart, com.google.android.material.R.attr.colorOnSurfaceVariant)
        val valueColor = MaterialColors.getColor(binding.statsChart, com.google.android.material.R.attr.colorOnSurface)

        return SpannableString(text).apply {
            setSpan(RelativeSizeSpan(0.8f), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(titleColor), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(RelativeSizeSpan(1.45f), title.length + 1, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(valueColor), title.length + 1, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
