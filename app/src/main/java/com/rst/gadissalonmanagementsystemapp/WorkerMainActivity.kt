package com.rst.gadissalonmanagementsystemapp

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.firestore.ListenerRegistration
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityWorkerMainBinding

class WorkerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerMainBinding
    private lateinit var navController: NavController
    private val mainViewModel: MainViewModel by viewModels()
    private var bookingsListener: ListenerRegistration? = null
    private var ordersListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // Tell the ViewModel to load the master list of hairstyles
        mainViewModel.loadAllHairstyles()

        // 1. Find the NavController from the NavHostFragment in the worker's layout
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.worker_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 2. Connect the BottomNavigationView to the NavController
        binding.bottomNavWorker.setupWithNavController(navController)

        // Call our new function to set up the UI listener
        setupNavigationListener()
    }

    override fun onStart() {
        super.onStart()
        // When the activity starts, begin listening for new bookings and orders
        listenForNotifications()
    }

    override fun onStop() {
        super.onStop()
        // When the activity stops, remove the listeners to save resources and prevent memory leaks
        bookingsListener?.remove()
        ordersListener?.remove()
        messagesListener?.remove()
    }

    private fun listenForNotifications() {
        // Listener for new booking requests
        bookingsListener = FirebaseManager.addPendingBookingsCountListener { count ->
            val badge = binding.bottomNavWorker.getOrCreateBadge(R.id.nav_worker_bookings)
            badge.isVisible = count > 0
            badge.number = count
        }

        // Listener for new product orders
        ordersListener = FirebaseManager.addPendingOrdersCountListener { count ->
            val badge = binding.bottomNavWorker.getOrCreateBadge(R.id.nav_worker_orders)
            badge.isVisible = count > 0
            badge.number = count
        }

        messagesListener = FirebaseManager.addWorkerUnreadMessageListener { count ->
            val badge = binding.bottomNavWorker.getOrCreateBadge(R.id.nav_worker_schedule)
            badge.isVisible = count > 0
            // We just show a dot, not a number, for unread messages.
            // This is a common pattern in chat apps.
        }
    }

    private fun setupNavigationListener() {
        // Handle the back button click
        binding.backButtonWorker.setOnClickListener {
            navController.navigateUp()
        }

        // Listen for screen changes to show/hide UI elements
        navController.addOnDestinationChangedListener { _, destination, _ ->

            // --- MODIFIED: Centralized Title and UI Logic ---
            val (title, showBack) = when (destination.id) {
                // Main BTM NAV Screens
                R.id.nav_worker_bookings -> Pair("New Bookings", false)
                R.id.nav_worker_schedule -> Pair("My Schedule", false)
                R.id.nav_worker_inventory -> Pair("Inventory", false)
                R.id.nav_worker_orders -> Pair("Product Orders", false)
                R.id.nav_worker_profile -> Pair("My Profile", false)

                // Profile Sub-Screens
                R.id.workerEditProfileFragment -> Pair("Edit Profile", true)
                R.id.workerTimeOffFragment -> Pair("My Time Off", true)
                R.id.workerTimeOffDialog -> Pair("Request Time Off", true)
                R.id.workerHelpCenterFragment -> Pair("Help & Support", true)
                R.id.workerMySupportTicketsFragment -> Pair("My Support Tickets", true)
                R.id.ticketDetailFragment -> Pair("Support Ticket", true)
                R.id.workerContactFragment -> Pair("Contact Support", true)
                R.id.workerSettingsFragment -> Pair("Settings", true)
                R.id.workerLocationFragment -> Pair("Salon Location", true)
                R.id.workerFaqFragment -> Pair("FAQ", true)
                R.id.workerAboutUsFragment -> Pair("About Us", true)

                // Other Detail Screens
                R.id.productDetailFragment -> Pair("Product Details", true)
                R.id.workerOrderDetailFragment -> Pair("Order Details", true)
                R.id.bookingDetailWorkerFragment -> Pair("Booking Details", true)

                // Default
                else -> Pair("Stylist Portal", false)
            }

            // Update UI based on the destination
            updateTitle(title)
            binding.bottomNavCardWorker.visibility = if (showBack) View.GONE else View.VISIBLE
            binding.backButtonWorker.visibility = if (showBack) View.VISIBLE else View.GONE
            // --- END MODIFICATION ---
        }
    }

    // --- NEW: Public functions to be called by fragments ---
    fun showBackButton(show: Boolean) {
        binding.backButtonWorker.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun updateTitle(title: String) {
        binding.titleWorker.text = title
    }
    // --- END NEW -
}