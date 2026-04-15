package com.heroconfigmanager.ui.hero

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.heroconfigmanager.R
import com.heroconfigmanager.data.model.Hero
import com.heroconfigmanager.databinding.ActivityHeroEditorBinding
import com.heroconfigmanager.ui.SharedViewModel

class HeroEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ROLE = "extra_role"
        const val EXTRA_HERO_JSON = "extra_hero_json"

        fun editIntent(context: Context, role: String, hero: Hero): Intent =
            Intent(context, HeroEditorActivity::class.java).apply {
                putExtra(EXTRA_ROLE, role)
                putExtra(EXTRA_HERO_JSON, com.google.gson.Gson().toJson(hero))
            }

        fun addIntent(context: Context, role: String): Intent =
            Intent(context, HeroEditorActivity::class.java).apply {
                putExtra(EXTRA_ROLE, role)
            }
    }

    private lateinit var binding: ActivityHeroEditorBinding
    lateinit var editorViewModel: HeroEditorViewModel
    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var role: String
    private var existingHero: Hero? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHeroEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets()

        role = intent.getStringExtra(EXTRA_ROLE) ?: run {
            finish()
            return
        }
        existingHero = intent.getStringExtra(EXTRA_HERO_JSON)?.let {
            runCatching { com.google.gson.Gson().fromJson(it, Hero::class.java) }.getOrNull()
        }

        editorViewModel = ViewModelProvider(this)[HeroEditorViewModel::class.java]
        editorViewModel.loadHero(existingHero)

        sharedViewModel = ViewModelProvider(
            (application as com.heroconfigmanager.HeroConfigApp).appViewModelStore,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[SharedViewModel::class.java]

        binding.editorToolbar.title = if (existingHero != null) {
            "Edit ${existingHero!!.hero}"
        } else {
            "Add Hero"
        }
        setSupportActionBar(binding.editorToolbar)

        val tabTitles = listOf("Basic Info", "Skins", "Upgrades")
        binding.editorViewPager.adapter = EditorPagerAdapter(this)
        TabLayoutMediator(binding.editorTabs, binding.editorViewPager) { tab, pos ->
            tab.text = tabTitles[pos]
        }.attach()

        binding.btnSaveHero.setOnClickListener {
            val hero = editorViewModel.buildHero()
            if (hero == null) {
                Toast.makeText(this, "Hero name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sharedViewModel.addOrUpdateHero(role, hero, existingHero)
            Toast.makeText(this, "${hero.hero} saved locally", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.editorToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun applyEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.updatePadding(top = systemBars.top)
            (binding.btnSaveHero.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.bottom_nav_margin_bottom) + systemBars.bottom
                binding.btnSaveHero.layoutParams = this
            }
            insets
        }
    }
}
