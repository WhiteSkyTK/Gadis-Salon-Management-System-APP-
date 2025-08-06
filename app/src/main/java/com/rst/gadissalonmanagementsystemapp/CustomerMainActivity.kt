package com.rst.gadissalonmanagementsystemapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityCustomerMainBinding // Make sure this import is correct

class CustomerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerMainBinding
    private lateinit var navController: NavController

    // Get an instance of the ViewModel
    private val mainViewModel: MainViewModel by viewModels()
    private var currentNavIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCustomerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupBottomNavClicks()
        setupNavigation()
    }

    private fun setupNavigation() {
        // --- CLICK LISTENERS FOR TOP ICONS ---
        binding.iconFavorites.setOnClickListener { navController.navigate(R.id.favoritesFragment) }
        binding.iconCart.setOnClickListener { navController.navigate(R.id.cartFragment) }
        binding.iconNotifications.setOnClickListener { navController.navigate(R.id.notificationsFragment) }
        binding.backButton.setOnClickListener { navController.navigateUp() }

        // --- ADD THIS BLOCK: Logic for the main favorite icon ---
        binding.iconFavoriteMain.setOnClickListener {
            // When the icon is clicked, tell the ViewModel
            mainViewModel.onFavoriteClicked()
        }
        mainViewModel.isCurrentProductFavorite.observe(this) { isFavorite ->
            // When the ViewModel's data changes, update the icon's image
            val iconRes = if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            binding.iconFavoriteMain.setImageResource(iconRes)
        }
        // --- END OF ADDED BLOCK ---

        // --- THE SINGLE, UNIFIED LISTENER FOR ALL NAVIGATION CHANGES ---
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Part 1: Handle Top Bar Appearance
            when (destination.id) {
                // Product/Hairstyle Detail Screens
                R.id.productDetailFragment,
                R.id.hairstyleDetailFragment -> {
                    showDetailTopBar()
                }

                // Profile Sub-screens
                R.id.settingsFragment,
                R.id.aboutUsFragment,
                R.id.contactFragment,
                R.id.locationFragment,
                R.id.favoritesFragment,
                R.id.cartFragment,
                R.id.notificationsFragment -> {
                    showProfileDetailTopBar()
                }

                // Main Screens
                else -> {
                    showHomeTopBar()
                }
            }

            // Part 2: Handle Bottom Nav Selection
            val navItems = listOf(
                Triple(binding.navHome, R.id.homeFragment, 0),
                Triple(binding.navShop, R.id.shopFragment, 1),
                Triple(binding.navBooking, R.id.bookingFragment, 2),
                Triple(binding.navProfile, R.id.profileFragment, 3)
            )
            navItems.forEach { (view, destinationId, index) ->
                if (destination.id == destinationId) {
                    view.isSelected = true
                    currentNavIndex = index
                } else {
                    view.isSelected = false
                }
            }
        }
    }

    private fun setupBottomNavClicks() {
        // This function now ONLY handles clicks.
        val navItems = listOf(
            Triple(binding.navHome, R.id.homeFragment, 0),
            Triple(binding.navShop, R.id.shopFragment, 1),
            Triple(binding.navBooking, R.id.bookingFragment, 2),
            Triple(binding.navProfile, R.id.profileFragment, 3)
        )

        navItems.forEach { (view, destinationId, targetIndex) ->
            view.setOnClickListener {
                if (navController.currentDestination?.id == destinationId) return@setOnClickListener

                val options = if (targetIndex > currentNavIndex) {
                    NavOptions.Builder()
                        .setEnterAnim(R.anim.slide_in_right).setExitAnim(R.anim.slide_out_left)
                        .setPopEnterAnim(R.anim.slide_in_left).setPopExitAnim(R.anim.slide_out_right)
                        .setPopUpTo(navController.graph.startDestinationId, false)
                        .build()
                } else {
                    NavOptions.Builder()
                        .setEnterAnim(R.anim.slide_in_left).setExitAnim(R.anim.slide_out_right)
                        .setPopEnterAnim(R.anim.slide_in_right).setPopExitAnim(R.anim.slide_out_left)
                        .setPopUpTo(navController.graph.startDestinationId, false)
                        .build()
                }
                navController.navigate(destinationId, null, options)
            }
        }
    }

    private fun showHomeTopBar() {
        binding.backButtonCard.visibility = View.GONE
        binding.bottomNavBar.visibility = View.VISIBLE
        // Show the group of home icons, hide the single favorite icon
        binding.iconFavorites.visibility = View.VISIBLE
        binding.iconCart.visibility = View.VISIBLE
        binding.iconNotifications.visibility = View.VISIBLE
        binding.iconFavoriteMain.visibility = View.GONE

        val params = binding.salonNameCard.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.05f
        binding.salonNameCard.layoutParams = params
    }

    private fun showDetailTopBar() {
        binding.backButtonCard.visibility = View.VISIBLE
        binding.bottomNavBar.visibility = View.GONE
        // Hide the group of home icons, show only the single favorite icon
        binding.iconFavorites.visibility = View.GONE
        binding.iconCart.visibility = View.GONE
        binding.iconNotifications.visibility = View.GONE
        binding.iconFavoriteMain.visibility = View.VISIBLE

        val params = binding.salonNameCard.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.5f
        binding.salonNameCard.layoutParams = params
    }

    private fun showProfileDetailTopBar() {
        binding.backButtonCard.visibility = View.VISIBLE
        binding.bottomNavBar.visibility = View.GONE

        // Show the original group of home icons
        binding.iconFavorites.visibility = View.VISIBLE
        binding.iconCart.visibility = View.VISIBLE
        binding.iconNotifications.visibility = View.VISIBLE
        // Hide the single favorite icon used on product pages
        binding.iconFavoriteMain.visibility = View.GONE

        // Center the logo
        val params = binding.salonNameCard.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.5f
        binding.salonNameCard.layoutParams = params
    }
}