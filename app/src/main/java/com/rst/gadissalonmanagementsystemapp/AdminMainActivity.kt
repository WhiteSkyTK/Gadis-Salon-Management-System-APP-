package com.rst.gadissalonmanagementsystemapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityAdminMainBinding

class AdminMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMainBinding
    private lateinit var navController: NavController

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

        logIdTokenClaims()

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
        val user = Firebase.auth.currentUser
        user?.getIdToken(true) // Force refresh the token
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val idTokenResult = task.result
                    val claims = idTokenResult?.claims
                    Log.d("TokenDebug", "--- User Token Claims ---")
                    claims?.forEach { (key, value) ->
                        Log.d("TokenDebug", "$key = $value")
                    }
                    Log.d("TokenDebug", "-------------------------")
                } else {
                    Log.e("TokenDebug", "Failed to get token", task.exception)
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
            when (destination.id) {
                R.id.adminAddProductFragment,
                R.id.adminAddHairstyleFragment,
                R.id.adminAddUserFragment,
                R.id.adminSettingsFragment,
                R.id.adminLocationFragment,
                R.id.adminAboutUsFragment,
                R.id.adminReplyMessageFragment,
                R.id.adminComposeMessageFragment,
                R.id.adminEditUserFragment,
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
}