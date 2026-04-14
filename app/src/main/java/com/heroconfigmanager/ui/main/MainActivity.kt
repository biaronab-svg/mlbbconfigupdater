package com.heroconfigmanager.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.heroconfigmanager.R
import com.heroconfigmanager.databinding.ActivityMainBinding
import com.heroconfigmanager.ui.SharedViewModel
import com.heroconfigmanager.ui.UiState

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge drawing so the app renders behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Use the application-scoped SharedViewModel so updates in EditorActivity are seen here
        viewModel = androidx.lifecycle.ViewModelProvider(
            (application as com.heroconfigmanager.HeroConfigApp).appViewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[SharedViewModel::class.java]

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        binding.bottomNav.setupWithNavController(navController)

        // ── Badge showing total hero count on the Heroes tab ──────────
        val badge = binding.bottomNav.getOrCreateBadge(R.id.heroesFragment)
        badge.isVisible = false
        badge.backgroundColor = getColor(R.color.error)
        badge.badgeTextColor = getColor(R.color.on_primary)
        // M3-style tight offsets
        val offsetPx = (4 * resources.displayMetrics.density).toInt()
        badge.verticalOffset  = offsetPx
        badge.horizontalOffset = offsetPx

        viewModel.config.observe(this) { config ->
            val total = config.roles.totalHeroes()
            badge.number    = total
            badge.isVisible = total > 0
        }

        // ── FAB visibility — show only on Heroes screen ──────────────
        // The FAB is anchored to the BottomNav in the layout, so it
        // automatically stays above it without any extra margin calculation.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.heroesFragment) {
                binding.fabAddHeroMain.show()
            } else {
                binding.fabAddHeroMain.hide()
            }
        }

        // ── Global error toast ────────────────────────────────────────
        viewModel.uiState.observe(this) { state ->
            if (state is UiState.Error) {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }

        // ── Last-sync subtitle in collapsing toolbar ──────────────────
        viewModel.lastSync.observe(this) { time ->
            binding.collapsingToolbar.title = if (time.isNotEmpty())
                "${getString(R.string.app_name)}  ·  $time"
            else
                getString(R.string.app_name)
        }

        // Load config on first launch only (not on rotation)
        if (savedInstanceState == null) {
            viewModel.fetchConfig()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.fetchConfig()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
