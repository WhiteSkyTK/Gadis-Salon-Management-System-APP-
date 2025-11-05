package com.rst.gadissalonmanagementsystemapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityAdminMainBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils

class AdminMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminMainBinding
    private lateinit var navController: NavController
    private var notificationListener: ListenerRegistration? = null
    private val mainViewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permission", "Notification permission granted.")
        } else {
            Log.d("Permission", "Notification permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // --- ADD THIS DEBUG LOG ---
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            Log.d("AdminDebug", "Current user is logged in. UID: ${currentUser.uid}, Email: ${currentUser.email}")
        } else {
            Log.e("AdminDebug", "CRITICAL ERROR: No user is logged in!")
        }

        askNotificationPermission()

        logIdTokenClaims()
        mainViewModel.loadAllHairstyles()

        // Find the NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.admin_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Connect the BottomNavigationView to the NavController
        binding.bottomNavAdmin.setupWithNavController(navController)

        // Setup our custom listener to manage UI changes
        setupNavigationListener()
        observeSupportMessagesForBadge()
    }


    private fun logIdTokenClaims() {
        Firebase.auth.currentUser?.getIdToken(true) // Force refresh
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val claims = task.result?.claims
                    Log.d("TokenDebug", "--- FRESH TOKEN CLAIMS ---")
                    claims?.forEach { (key, value) ->
                        Log.d("TokenDebug", "$key = $value")
                    }
                    Log.d("TokenDebug", "--------------------------")
                } else {
                    Log.e("TokenDebug", "Failed to get fresh token", task.exception)
                }
            }
    }

    private fun observeSupportMessagesForBadge() {
        // This will listen for real-time changes in your support messages
        Firebase.firestore.collection("support_messages")
            .whereEqualTo("status", "New") // We only care about "New" messages
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("AdminMain", "Listen failed.", error)
                    return@addSnapshotListener
                }

                val unreadCount = snapshots?.size() ?: 0

                val badge = binding.bottomNavAdmin.getOrCreateBadge(R.id.nav_admin_profile)

                if (unreadCount > 0) {
                    badge.isVisible = true
                    badge.number = unreadCount
                } else {
                    badge.isVisible = false
                }
            }
    }

    private fun setupNavigationListener() {
        // Handle the back button click
        binding.backButtonAdmin.setOnClickListener {
            navController.navigateUp()
        }

        // Listen for screen changes
        navController.addOnDestinationChangedListener { _, destination, _ ->

            // --- MODIFIED: Set title based on destination ID ---
            val title = when (destination.id) {
                // Main BTM NAV screens
                R.id.nav_admin_dashboard -> "Admin Panel"
                R.id.nav_admin_products -> "Products & Services"
                R.id.nav_admin_users -> "User Management"
                R.id.nav_admin_bookings -> "Bookings & Orders"
                R.id.nav_admin_profile -> "My Profile"

                // Sub-screens (that hide the BTM NAV)
                R.id.nav_admin_income -> "Income Report"
                R.id.nav_admin_timeoff -> "Time Off Management"
                R.id.adminTimeOffDialog -> "Time Off Request" // (Assuming you added this ID to nav graph)
                R.id.adminEditHairstyleFragment -> "Edit Hairstyle"
                R.id.adminEditProductFragment -> "Edit Product"
                R.id.adminEditProfileFragment -> "Edit Profile"
                R.id.adminAddProductFragment -> "Add Product"
                R.id.adminAddHairstyleFragment -> "Add Hairstyle"
                R.id.adminAddUserFragment -> "Add User"
                R.id.adminSettingsFragment -> "Settings"
                R.id.adminLocationFragment -> "Manage Location"
                R.id.adminAboutUsFragment -> "Edit About Us"
                R.id.adminTicketDetailFragment -> "Support Ticket"
                R.id.adminComposeMessageFragment -> "Compose Message"
                R.id.adminEditUserFragment -> "Edit User"
                R.id.adminFaqFragment -> "Manage FAQs"
                R.id.adminBookingDetailFragment -> "Booking Details"
                R.id.adminOrderDetailFragment -> "Order Details"
                R.id.adminSupportFragment -> "Support"

                // Default fallback
                else -> "Admin Panel"
            }
            updateTitle(title) // Call your updateTitle function


            when (destination.id) {
                R.id.nav_admin_income,
                R.id.nav_admin_timeoff,
                R.id.adminTimeOffDialog, // (Assuming you added this ID)
                R.id.adminEditHairstyleFragment,
                R.id.adminEditProductFragment,
                R.id.adminEditProfileFragment,
                R.id.adminAddProductFragment,
                R.id.adminAddHairstyleFragment,
                R.id.adminAddUserFragment,
                R.id.adminSettingsFragment,
                R.id.adminLocationFragment,
                R.id.adminAboutUsFragment,
                R.id.adminTicketDetailFragment,
                R.id.adminComposeMessageFragment,
                R.id.adminEditUserFragment,
                R.id.adminFaqFragment,
                R.id.adminBookingDetailFragment,
                R.id.adminOrderDetailFragment,
                R.id.adminSupportFragment -> {
                    binding.bottomNavCardAdmin.visibility = View.GONE
                    binding.backButtonAdmin.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavCardAdmin.visibility = View.VISIBLE
                    binding.backButtonAdmin.visibility = View.GONE
                }
            }
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API 33+ (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is already granted
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- NEW: Public functions to be called by fragments ---
    fun showBackButton(show: Boolean) {
        binding.backButtonAdmin.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun updateTitle(title: String) {
        binding.titleAdmin.text = title
    }
}