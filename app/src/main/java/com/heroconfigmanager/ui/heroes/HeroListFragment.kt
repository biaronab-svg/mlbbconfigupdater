package com.heroconfigmanager.ui.heroes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
            onEdit   = { hero -> openEditor(hero) },
            onDelete = { hero -> confirmDelete(hero) }
        )
        binding.heroRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.heroRecyclerView.adapter = adapter

        viewModel.config.observe(viewLifecycleOwner) { config ->
            val heroes = config.roles.heroesFor(role)
            adapter.submitList(heroes)
            binding.emptyState.visibility = if (heroes.isEmpty()) View.VISIBLE else View.GONE
            binding.heroRecyclerView.visibility = if (heroes.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun openEditor(hero: Hero) {
        // Use the factory that serialises the full Hero object into the Intent,
        // so HeroEditorActivity can pre-populate the editor regardless of
        // whether the SharedViewModel has already loaded the config.
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
        _binding = null
    }
}
