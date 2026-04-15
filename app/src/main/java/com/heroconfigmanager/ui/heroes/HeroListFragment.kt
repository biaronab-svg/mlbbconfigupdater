package com.heroconfigmanager.ui.heroes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heroconfigmanager.R
import com.heroconfigmanager.data.model.Hero
import com.heroconfigmanager.databinding.FragmentHeroListBinding
import com.heroconfigmanager.ui.SharedViewModel
import com.heroconfigmanager.ui.hero.HeroEditorActivity

class HeroListFragment : Fragment() {

    companion object {
        const val ARG_ROLE = "arg_role"
    }

    private var _binding: FragmentHeroListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedViewModel
    private lateinit var adapter: HeroAdapter
    private lateinit var role: String
    private var currentHeroes: List<Hero> = emptyList()
    private var isFetchingConfig: Boolean = false
    private var hasFinishedInitialLoad: Boolean = false
    private var skeleton: Skeleton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHeroListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        role = arguments?.getString(ARG_ROLE) ?: return

        viewModel = androidx.lifecycle.ViewModelProvider(
            (requireActivity().application as com.heroconfigmanager.HeroConfigApp).appViewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SharedViewModel::class.java]

        adapter = HeroAdapter(
            onEdit = { hero -> openEditor(hero) },
            onDelete = { hero -> confirmDelete(hero) }
        )
        binding.heroRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.heroRecyclerView.adapter = adapter
        skeleton = binding.heroRecyclerView.applySkeleton(R.layout.item_hero)

        viewModel.config.observe(viewLifecycleOwner) { config ->
            currentHeroes = config.roles.heroesFor(role)
            adapter.submitList(currentHeroes)
            renderState()
        }

        viewModel.isFetchingConfig.observe(viewLifecycleOwner) { loading ->
            isFetchingConfig = loading
            renderState()
        }

        viewModel.hasFinishedInitialLoad.observe(viewLifecycleOwner) { finished ->
            hasFinishedInitialLoad = finished
            renderState()
        }
    }

    private fun renderState() {
        val showSkeleton = isFetchingConfig && !hasFinishedInitialLoad && currentHeroes.isEmpty()
        if (showSkeleton) {
            binding.heroRecyclerView.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            skeleton?.showSkeleton()
            return
        }

        skeleton?.showOriginal()
        val showEmpty = currentHeroes.isEmpty()
        binding.emptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.heroRecyclerView.visibility = if (showEmpty) View.GONE else View.VISIBLE
    }

    private fun openEditor(hero: Hero) {
        startActivity(HeroEditorActivity.editIntent(requireContext(), role, hero))
    }

    private fun confirmDelete(hero: Hero) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Hero")
            .setMessage("Remove ${hero.hero} from $role? This is a local change.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteHero(role, hero) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        skeleton = null
        _binding = null
    }
}
