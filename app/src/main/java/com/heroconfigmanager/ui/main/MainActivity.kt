package com.heroconfigmanager.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.color.MaterialColors
import com.heroconfigmanager.R
import com.heroconfigmanager.databinding.ActivityMainBinding
import com.heroconfigmanager.ui.SharedViewModel
import com.heroconfigmanager.ui.UiState

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: SharedViewModel
    private var lastSyncText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
        val useNavigationRail = resources.getBoolean(R.bool.use_navigation_rail)

        binding.railCard.isVisible = useNavigationRail
        binding.bottomNavCard.isVisible = !useNavigationRail

        if (useNavigationRail) {
            binding.navigationRail.setupWithNavController(navController)
        } else {
            binding.bottomNav.setupWithNavController(navController)
        }

        applyEdgeToEdgeInsets(useNavigationRail)

        val badge = binding.bottomNav.getOrCreateBadge(R.id.heroesFragment)
        badge.isVisible = false
        badge.backgroundColor = MaterialColors.getColor(binding.bottomNav, com.google.android.material.R.attr.colorError)
        badge.badgeTextColor = MaterialColors.getColor(binding.bottomNav, com.google.android.material.R.attr.colorOnError)
        val badgeOffset = (4 * resources.displayMetrics.density).toInt()
        badge.verticalOffset = badgeOffset
        badge.horizontalOffset = badgeOffset
        if (useNavigationRail) {
            binding.navigationRail.getOrCreateBadge(R.id.heroesFragment).apply {
                isVisible = false
                backgroundColor = badge.backgroundColor
                badgeTextColor = badge.badgeTextColor
                verticalOffset = badgeOffset
                horizontalOffset = badgeOffset
            }
        }

        viewModel.config.observe(this) { config ->
            val total = config.roles.totalHeroes()
            badge.number = total
            badge.isVisible = total > 0
            if (useNavigationRail) {
                binding.navigationRail.getOrCreateBadge(R.id.heroesFragment).apply {
                    number = total
                    isVisible = total > 0
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.fabAddHeroMain.isVisible = destination.id == R.id.heroesFragment
            binding.fabAddHeroMain.text = getString(R.string.add_hero)
            binding.collapsingToolbar.title = destination.label?.toString() ?: getString(R.string.app_name)
            updateToolbarSubtitle()
        }

        viewModel.uiState.observe(this) { state ->
            if (state is UiState.Error) {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.lastSync.observe(this) { time ->
            lastSyncText = time
            updateToolbarSubtitle()
        }

        if (savedInstanceState == null) {
            viewModel.fetchConfig()
        }
    }

    private fun updateToolbarSubtitle() {
        binding.toolbar.subtitle = if (lastSyncText.isNotEmpty()) {
            getString(R.string.main_toolbar_subtitle_synced, lastSyncText)
        } else {
            null
        }
    }

    private fun applyEdgeToEdgeInsets(useNavigationRail: Boolean) {
        val bottomNavHorizontal = resources.getDimensionPixelSize(R.dimen.bottom_nav_margin_horizontal)
        val bottomNavBottom = resources.getDimensionPixelSize(R.dimen.bottom_nav_margin_bottom)
        val fabMarginEnd = resources.getDimensionPixelSize(R.dimen.fab_margin_end)
        val fabMarginBottom = resources.getDimensionPixelSize(R.dimen.fab_margin_bottom)
        val fabNavGap = resources.getDimensionPixelSize(R.dimen.fab_nav_gap)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.appBarLayout.updatePadding(top = systemBars.top)

            if (useNavigationRail) {
                (binding.railCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
                    topMargin = 12.dp + systemBars.top
                    bottomMargin = 12.dp + systemBars.bottom
                    leftMargin = 12.dp + systemBars.left
                    binding.railCard.layoutParams = this
                }
            } else {
                (binding.bottomNavCard.layoutParams as CoordinatorLayout.LayoutParams).apply {
                    leftMargin = bottomNavHorizontal + systemBars.left
                    rightMargin = bottomNavHorizontal + systemBars.right
                    bottomMargin = bottomNavBottom + systemBars.bottom
                    binding.bottomNavCard.layoutParams = this
                }
            }

            updateFabPosition(
                useNavigationRail = useNavigationRail,
                fabMarginEnd = fabMarginEnd,
                fabMarginBottom = fabMarginBottom,
                fabNavGap = fabNavGap,
                systemBars = systemBars
            )

            insets
        }
    }

    private fun updateFabPosition(
        useNavigationRail: Boolean,
        fabMarginEnd: Int,
        fabMarginBottom: Int,
        fabNavGap: Int,
        systemBars: androidx.core.graphics.Insets,
    ) {
        if (useNavigationRail) {
            (binding.fabAddHeroMain.layoutParams as CoordinatorLayout.LayoutParams).apply {
                anchorId = View.NO_ID
                anchorGravity = 0
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                marginEnd = fabMarginEnd + systemBars.right
                bottomMargin = fabMarginBottom + systemBars.bottom
                binding.fabAddHeroMain.layoutParams = this
            }
            return
        }

        binding.bottomNavCard.doOnLayout {
            (binding.fabAddHeroMain.layoutParams as CoordinatorLayout.LayoutParams).apply {
                anchorId = View.NO_ID
                anchorGravity = 0
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                marginEnd = fabMarginEnd + systemBars.right
                bottomMargin = binding.bottomNavCard.height +
                    resources.getDimensionPixelSize(R.dimen.bottom_nav_margin_bottom) +
                    systemBars.bottom +
                    fabNavGap
                binding.fabAddHeroMain.layoutParams = this
            }
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

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
