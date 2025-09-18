package com.rst.gadissalonmanagementsystemapp

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityCustomerMainBinding

class CustomerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerMainBinding
    private lateinit var navController: NavController
    private val mainViewModel: MainViewModel by viewModels()
    private var currentNavIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Call both setup functions
        mainViewModel.loadCurrentUser()
        setupBottomNavigation()
        setupUiVisibilityListener()
    }

    private fun setupUiVisibilityListener() {
        // --- CLICK LISTENERS FOR TOP ICONS ---
        binding.iconFavorites.setOnClickListener { navController.navigate(R.id.favoritesFragment) }
        binding.iconCart.setOnClickListener { navController.navigate(R.id.cartFragment) }
        binding.iconNotifications.setOnClickListener { navController.navigate(R.id.notificationsFragment) }
        binding.backButton.setOnClickListener { navController.navigateUp() }

        // --- Logic for the main favorite icon ---
        binding.iconFavoriteMain.setOnClickListener {
            mainViewModel.onFavoriteClicked()
        }
        mainViewModel.isCurrentProductFavorite.observe(this) { isFavorite ->
            val iconRes = if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            binding.iconFavoriteMain.setImageResource(iconRes)
        }

        // --- LISTENER FOR UI CHANGES ---
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Part 1: Handle Top Bar Appearance
            when (destination.id) {
                R.id.productDetailFragment,
                R.id.hairstyleDetailFragment, R.id.bookingConfirmationFragment -> showDetailTopBar()
                R.id.settingsFragment,
                R.id.aboutUsFragment,
                R.id.contactFragment,
                R.id.locationFragment,
                R.id.favoritesFragment,
                R.id.cartFragment,
                R.id.notificationsFragment -> showProfileDetailTopBar()
                else -> showHomeTopBar()
            }

            // Part 2: Handle Bottom Nav Selection
            when (destination.id) {
                R.id.homeFragment -> binding.bottomNavView.selectedItemId = R.id.homeFragment
                R.id.shopFragment -> binding.bottomNavView.selectedItemId = R.id.shopFragment
                R.id.bookingFragment -> binding.bottomNavView.selectedItemId = R.id.bookingFragment
                R.id.customerOrdersFragment -> binding.bottomNavView.selectedItemId = R.id.customerOrdersFragment
                R.id.profileFragment -> binding.bottomNavView.selectedItemId = R.id.profileFragment
            }
        }
    }

    private fun setupBottomNavigation() {
        // This listener handles the clicks on your bottom navigation items
        binding.bottomNavView.setOnItemSelectedListener { item ->
            // Find the index of the clicked item
            val targetIndex = when (item.itemId) {
                R.id.homeFragment -> 0
                R.id.shopFragment -> 1
                R.id.bookingFragment -> 2
                R.id.customerOrdersFragment -> 3
                R.id.profileFragment -> 4
                else -> -1
            }

            if (targetIndex == -1 || navController.currentDestination?.id == item.itemId) {
                return@setOnItemSelectedListener false // Do nothing if item is invalid or already selected
            }

            // Determine animation direction
            val options = if (targetIndex > currentNavIndex) {
                NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right).setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left).setPopExitAnim(R.anim.slide_out_right)
                    .setPopUpTo(navController.graph.startDestinationId, false).build()
            } else {
                NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_left).setExitAnim(R.anim.slide_out_right)
                    .setPopEnterAnim(R.anim.slide_in_right).setPopExitAnim(R.anim.slide_out_left)
                    .setPopUpTo(navController.graph.startDestinationId, false).build()
            }

            navController.navigate(item.itemId, null, options)
            currentNavIndex = targetIndex // Update the current index

            true // Return true to show the item as selected
        }
    }

    private fun showHomeTopBar() {
        binding.backButtonCard.visibility = View.GONE
        binding.bottomNavBar.visibility = View.VISIBLE
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
        binding.iconFavorites.visibility = View.VISIBLE
        binding.iconCart.visibility = View.VISIBLE
        binding.iconNotifications.visibility = View.VISIBLE
        binding.iconFavoriteMain.visibility = View.GONE
        val params = binding.salonNameCard.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.5f
        binding.salonNameCard.layoutParams = params
    }
}

