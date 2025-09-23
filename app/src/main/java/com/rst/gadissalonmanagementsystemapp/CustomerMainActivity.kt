package com.rst.gadissalonmanagementsystemapp

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityCustomerMainBinding

class CustomerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerMainBinding
    private lateinit var navController: NavController
    val mainViewModel: MainViewModel by viewModels()
    private var currentNavIndex = 0
    private var notificationListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        mainViewModel.loadCurrentUser()
        mainViewModel.loadAllHairstyles()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupBottomNavigation()
        setupUiVisibilityListener()
        setupPullToRefresh()
    }

    override fun onStart() {
        super.onStart()
        // Start listening for unread notifications
        notificationListener = FirebaseManager.addUnreadNotificationsListener { unreadCount ->
            binding.notificationDot.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
        notificationListener?.remove()
    }

    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // When the user pulls to refresh, reload all the core data
            mainViewModel.loadCurrentUser()
            mainViewModel.loadAllHairstyles()
            // You can add other data reloads here (e.g., products)

            // Hide the refreshing indicator after a short delay
            binding.swipeRefreshLayout.postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, 1500)
        }
    }

    private fun setupUiVisibilityListener() {
        // Create the NavOptions that will clear the back stack
        val navOptions = NavOptions.Builder()
            .setPopUpTo(navController.graph.startDestinationId, false)
            .build()

        binding.iconFavorites.setOnClickListener {
            if (navController.currentDestination?.id == R.id.favoritesFragment) {
                navController.popBackStack() // If already open, go back to home
            } else {
                navController.navigate(R.id.favoritesFragment, null, navOptions)
            }
        }
        binding.iconCart.setOnClickListener {
            if (navController.currentDestination?.id == R.id.cartFragment) {
                navController.popBackStack()
            } else {
                navController.navigate(R.id.cartFragment, null, navOptions)
            }
        }
        binding.iconNotifications.setOnClickListener {
            if (navController.currentDestination?.id == R.id.notificationsFragment) {
                navController.popBackStack()
            } else {
                navController.navigate(R.id.notificationsFragment, null, navOptions)
            }
        }
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
            when (destination.id) {
                R.id.productDetailFragment, R.id.hairstyleDetailFragment -> showDetailTopBar()

                R.id.settingsFragment, R.id.aboutUsFragment, R.id.contactFragment,
                R.id.locationFragment, R.id.customerEditProfileFragment, R.id.orderDetailFragment,
                R.id.purchaseConfirmationFragment, R.id.bookingConfirmationFragment,
                R.id.helpCenterFragment, R.id.mySupportTicketsFragment,
                R.id.faqFragment -> showSimpleDetailTopBar() // Use the new, simpler top bar

                R.id.favoritesFragment, R.id.cartFragment, R.id.notificationsFragment -> showProfileDetailTopBar()

                else -> showHomeTopBar()
            }

            // Part 2: Set the selected state for the top nav icons
            binding.iconFavorites.isSelected = (destination.id == R.id.favoritesFragment)
            binding.iconCart.isSelected = (destination.id == R.id.cartFragment)
            binding.iconNotifications.isSelected = (destination.id == R.id.notificationsFragment)


            // Part 3: Handle Bottom Nav Selection
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

    private fun animateViewVisibility(view: View, show: Boolean) {
        // Use slide animations for the icon groups
        val animationId = if (show) R.anim.slide_in_from_right else R.anim.slide_out_to_right

        // Use fade for the back button for a cleaner look
        val backButtonAnimationId = if (show) R.anim.fade_in else R.anim.fade_out

        // Check the view's ID to apply the correct animation
        val animToUse = when (view.id) {
            R.id.back_button_card -> backButtonAnimationId
            else -> animationId
        }

        if ((show && view.visibility != View.VISIBLE) || (!show && view.visibility == View.VISIBLE)) {
            view.startAnimation(AnimationUtils.loadAnimation(this, animToUse))
            view.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun showHomeTopBar() {
        animateViewVisibility(binding.backButtonCard, false)
        binding.bottomNavBar.visibility = View.VISIBLE // Corrected from bottom_nav_bar
        animateViewVisibility(binding.homeIconsGroup, true)
        animateViewVisibility(binding.iconFavoriteMain, false)

        val params = binding.salonNameCard.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.05f
        binding.salonNameCard.layoutParams = params
    }

    private fun showDetailTopBar() {
        animateViewVisibility(binding.backButtonCard, true)
        binding.bottomNavBar.visibility = View.GONE
        animateViewVisibility(binding.homeIconsGroup, false)
        animateViewVisibility(binding.iconFavoriteMain, true)

        val params = binding.salonNameCard.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.5f
        binding.salonNameCard.layoutParams = params
    }

    private fun showSimpleDetailTopBar() {
        animateViewVisibility(binding.backButtonCard, true)
        binding.bottomNavBar.visibility = View.GONE
        // Hide ALL right-side icons
        animateViewVisibility(binding.homeIconsGroup, false)
        animateViewVisibility(binding.iconFavoriteMain, false)

        val params = binding.salonNameCard.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.5f
        binding.salonNameCard.layoutParams = params
    }

    private fun showProfileDetailTopBar() {
        animateViewVisibility(binding.backButtonCard, true)
        binding.bottomNavBar.visibility = View.GONE
        animateViewVisibility(binding.homeIconsGroup, true)
        animateViewVisibility(binding.iconFavoriteMain, false)

        val params = binding.salonNameCard.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.5f
        binding.salonNameCard.layoutParams = params
    }
}