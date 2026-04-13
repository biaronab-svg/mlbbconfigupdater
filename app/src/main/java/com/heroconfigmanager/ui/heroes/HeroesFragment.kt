package com.heroconfigmanager.ui.heroes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.heroconfigmanager.R
import com.heroconfigmanager.data.model.HERO_ROLES
import com.heroconfigmanager.databinding.FragmentHeroesBinding
import com.heroconfigmanager.ui.SharedViewModel
import com.heroconfigmanager.ui.UiState
import com.heroconfigmanager.ui.hero.HeroEditorActivity

class HeroesFragment : Fragment() {

    private var _binding: FragmentHeroesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHeroesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = androidx.lifecycle.ViewModelProvider(
            (requireActivity().application as com.heroconfigmanager.HeroConfigApp).appViewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SharedViewModel::class.java]

        val pagerAdapter = RolePagerAdapter(this, HERO_ROLES)
        binding.heroViewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.roleTabs, binding.heroViewPager) { tab, position ->
            tab.text = HERO_ROLES[position]
        }.attach()

        // Swipe-to-refresh = fetch FROM GitHub
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchConfig()
        }

        // Handle activity-level FAB from fragment
        val fab = requireActivity().findViewById<ExtendedFloatingActionButton>(R.id.fabAddHeroMain)
        
        fab?.let {
            // Animate FAB entrance with scale+overshoot
            it.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fab_scale_up)
            )

            // FAB = add new hero for currently selected role
            it.setOnClickListener {
                val role = HERO_ROLES[binding.heroViewPager.currentItem]
                startActivity(HeroEditorActivity.addIntent(requireContext(), role))
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = state is UiState.Loading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
