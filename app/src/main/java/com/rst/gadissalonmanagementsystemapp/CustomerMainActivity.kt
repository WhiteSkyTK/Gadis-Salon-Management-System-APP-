package com.rst.gadissalonmanagementsystemapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityCustomerMainBinding // Make sure this import is correct

class CustomerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerMainBinding
    private lateinit var navController: NavController

    private var currentNavIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCustomerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupTopNavigation()
        setupBottomNavigation()
    }

    private fun setupTopNavigation() {
        // Handle clicks for the right-side icons
        binding.iconFavorites.setOnClickListener { navController.navigate(R.id.favoritesFragment) }
        binding.iconCart.setOnClickListener { navController.navigate(R.id.cartFragment) }
        binding.iconNotifications.setOnClickListener { navController.navigate(R.id.notificationsFragment) }

        // Handle clicks for the new back button
        binding.backButton.setOnClickListener {
            navController.navigateUp() // Standard way to go back
        }

        // The magic happens here: Listen for screen changes to update the UI
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // These are the "secondary" screens
                R.id.favoritesFragment,
                R.id.cartFragment,
                R.id.productDetailFragment,
                R.id.notificationsFragment -> {
                    showDetailTopBar()
                }
                // All other screens are "main" screens
                else -> {
                    showHomeTopBar()
                }
            }
        }
    }

    private fun showHomeTopBar() {
        binding.backButtonCard.visibility = View.GONE
        binding.iconsCard.visibility = View.VISIBLE
        binding.bottomNavBar.visibility = View.VISIBLE

        // Change the constraints to move the logo to the left
        val params = binding.salonNameCard.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.05f // A small bias to give it some margin
        binding.salonNameCard.layoutParams = params
    }

    private fun showDetailTopBar() {
        binding.backButtonCard.visibility = View.VISIBLE
        binding.iconsCard.visibility = View.VISIBLE // KEEP VISIBLE AS REQUESTED
        binding.bottomNavBar.visibility = View.GONE

        // Change the constraints to move the logo to the center
        val params = binding.salonNameCard.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.5f // 0.5 centers the view
        binding.salonNameCard.layoutParams = params
    }
    private fun setupBottomNavigation() {
        // We now store the index of each item (0=Home, 1=Shop, etc.)
        val navItems = listOf(
            Triple(binding.navHome, R.id.homeFragment, 0),
            Triple(binding.navShop, R.id.shopFragment, 1),
            Triple(binding.navBooking, R.id.bookingFragment, 2),
            Triple(binding.navProfile, R.id.profileFragment, 3)
        )

        navItems.forEach { (view, destinationId, targetIndex) ->
            view.setOnClickListener {
                // Determine animation direction
                val options = if (targetIndex > currentNavIndex) {
                    // Navigating forward (e.g., Home -> Shop)
                    NavOptions.Builder()
                        .setEnterAnim(R.anim.slide_in_right)
                        .setExitAnim(R.anim.slide_out_left)
                        .setPopEnterAnim(R.anim.slide_in_left)
                        .setPopExitAnim(R.anim.slide_out_right)
                        .setPopUpTo(navController.graph.startDestinationId, false)
                        .build()
                } else {
                    // Navigating backward (e.g., Profile -> Home)
                    NavOptions.Builder()
                        .setEnterAnim(R.anim.slide_in_left)
                        .setExitAnim(R.anim.slide_out_right)
                        .setPopEnterAnim(R.anim.slide_in_right)
                        .setPopExitAnim(R.anim.slide_out_left)
                        .setPopUpTo(navController.graph.startDestinationId, false)
                        .build()
                }

                navController.navigate(destinationId, null, options)
            }
        }

        // We need to update two things when the destination changes:
        // 1. Which icon is selected.
        // 2. The currentNavIndex for the next click.
        navController.addOnDestinationChangedListener { _, destination, _ ->
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
}