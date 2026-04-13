package com.heroconfigmanager.ui.hero

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.heroconfigmanager.R
import com.heroconfigmanager.data.model.Hero
import com.heroconfigmanager.databinding.ActivityHeroEditorBinding
import com.heroconfigmanager.ui.SharedViewModel

class HeroEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ROLE       = "extra_role"
        const val EXTRA_HERO_JSON  = "extra_hero_json"

        /** Call this to open the editor for an EXISTING hero. */
        fun editIntent(context: Context, role: String, hero: Hero): Intent =
            Intent(context, HeroEditorActivity::class.java).apply {
                putExtra(EXTRA_ROLE, role)
                // Serialize the full hero as JSON so the activity-scoped
                // ViewModel can be pre-populated without needing the
                // app-scoped SharedViewModel (which is empty in a new process).
                putExtra(EXTRA_HERO_JSON, com.google.gson.Gson().toJson(hero))
            }

        /** Call this to open the editor for a NEW hero. */
        fun addIntent(context: Context, role: String): Intent =
            Intent(context, HeroEditorActivity::class.java).apply {
                putExtra(EXTRA_ROLE, role)
            }
    }

    private lateinit var binding: ActivityHeroEditorBinding

    // Activity-scoped ViewModel for editing state (tabs share this)
    lateinit var editorViewModel: HeroEditorViewModel

    // App-scoped SharedViewModel to persist the result back
    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var role: String
    private var existingHero: Hero? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Slide-up entrance, slide-down exit
        overridePendingTransition(R.anim.slide_in_bottom, android.R.anim.fade_out)
        binding = ActivityHeroEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        role = intent.getStringExtra(EXTRA_ROLE) ?: run { finish(); return }

        // Deserialize existing hero from Intent JSON (avoids cross-process ViewModel issue)
        val heroJson = intent.getStringExtra(EXTRA_HERO_JSON)
        existingHero = heroJson?.let {
            runCatching { com.google.gson.Gson().fromJson(it, Hero::class.java) }.getOrNull()
        }

        editorViewModel = ViewModelProvider(this)[HeroEditorViewModel::class.java]
        editorViewModel.loadHero(existingHero)

        // Use the application-scoped SharedViewModel so we write back to the live config
        sharedViewModel = ViewModelProvider(
            (application as com.heroconfigmanager.HeroConfigApp).appViewModelStore,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[SharedViewModel::class.java]

        val title = if (existingHero != null) "Edit ${existingHero!!.hero}" else "Add Hero"
        binding.editorToolbar.title = title
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

        binding.editorToolbar.setNavigationOnClickListener { finish() }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_in_bottom)
    }
}
