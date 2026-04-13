package com.heroconfigmanager.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

        // Improve BottomNav with a Badge for hero count
        val badge = binding.bottomNav.getOrCreateBadge(R.id.heroesFragment)
        badge.isVisible = false // Hidden initially
        badge.backgroundColor = getColor(R.color.error)
        badge.badgeTextColor = getColor(R.color.on_primary)
        
        // Add subtle offsets for tighter M3 look (4dp)
        val offset = (4 * resources.displayMetrics.density).toInt()
        badge.verticalOffset = offset
        badge.horizontalOffset = offset
        
        viewModel.config.observe(this) { config ->
            val total = config.roles.totalHeroes()
            badge.number = total
            badge.isVisible = total > 0
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.heroesFragment) {
                binding.fabAddHeroMain.show()
            } else {
                binding.fabAddHeroMain.hide()
            }
        }

        // Show loading/error feedback globally
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Error ->
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                else -> Unit
            }
        }

        viewModel.lastSync.observe(this) { time ->
            supportActionBar?.subtitle = if (time.isNotEmpty()) "Synced $time" else null
        }

        // Load config on first launch
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
