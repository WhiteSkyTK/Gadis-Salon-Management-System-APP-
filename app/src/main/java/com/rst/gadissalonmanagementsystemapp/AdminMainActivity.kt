package com.rst.gadissalonmanagementsystemapp

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityAdminMainBinding

class AdminMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // Find the NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.admin_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Connect the BottomNavigationView to the NavController
        binding.bottomNavAdmin.setupWithNavController(navController)

        // Setup our custom listener to manage UI changes
        setupNavigationListener()
    }

    private fun setupNavigationListener() {
        // Handle the back button click
        binding.backButtonAdmin.setOnClickListener {
            navController.navigateUp()
        }

        // Listen for screen changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // ADD THE "ADD" FRAGMENT IDs TO THIS LIST
                R.id.adminAddProductFragment,
                R.id.adminAddHairstyleFragment,
                R.id.adminAddUserFragment -> {
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