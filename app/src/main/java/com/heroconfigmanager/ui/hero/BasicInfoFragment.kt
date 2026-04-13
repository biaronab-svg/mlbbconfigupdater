package com.heroconfigmanager.ui.hero

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.heroconfigmanager.databinding.FragmentEditorBasicBinding

class BasicInfoFragment : Fragment() {

    private var _binding: FragmentEditorBasicBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: HeroEditorViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditorBasicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm = ViewModelProvider(requireActivity())[HeroEditorViewModel::class.java]

        // Populate fields from VM
        binding.etHeroName.setText(vm.heroName)
        binding.etHeroTitle.setText(vm.heroTitle)
        binding.etHeroType.setText(vm.heroType)
        binding.etHeroLogo.setText(vm.heroLogo)
        binding.etHeroSplash.setText(vm.heroSplash)
        binding.etRestoreUrl.setText(vm.restoreUrl)

        // Write back to VM on every keystroke
        binding.etHeroName.doAfterTextChanged   { vm.heroName   = it.toString() }
        binding.etHeroTitle.doAfterTextChanged  { vm.heroTitle  = it.toString() }
        binding.etHeroType.doAfterTextChanged   { vm.heroType   = it.toString() }
        binding.etHeroLogo.doAfterTextChanged   { vm.heroLogo   = it.toString() }
        binding.etHeroSplash.doAfterTextChanged { vm.heroSplash = it.toString() }
        binding.etRestoreUrl.doAfterTextChanged { vm.restoreUrl = it.toString() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
