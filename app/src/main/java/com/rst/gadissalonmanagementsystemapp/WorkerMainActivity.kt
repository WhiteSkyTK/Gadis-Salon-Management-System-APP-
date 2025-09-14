package com.rst.gadissalonmanagementsystemapp

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityWorkerMainBinding

class WorkerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkerMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // 1. Find the NavController from the NavHostFragment in the worker's layout
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.worker_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 2. Connect the BottomNavigationView to the NavController
        binding.bottomNavWorker.setupWithNavController(navController)

        // Call our new function to set up the UI listener
        setupNavigationListener()
    }

    private fun setupNavigationListener() {
        // Handle the back button click
        binding.backButtonWorker.setOnClickListener {
            navController.navigateUp()
        }

        // Listen for screen changes to show/hide UI elements
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // These are the "detail" screens where we want a back button
            if (destination.id == R.id.workerSettingsFragment ||
                destination.id == R.id.workerEditProfileFragment ||
                destination.id == R.id.workerAboutUsFragment ||
                destination.id == R.id.workerLocationFragment ||
                destination.id == R.id.workerHelpCenterFragment ||
                destination.id == R.id.workerMySupportTicketsFragment ||
                destination.id == R.id.workerFaqFragment ||
                destination.id == R.id.productDetailFragment ||
                destination.id == R.id.workerContactFragment) {

                binding.bottomNavCardWorker.visibility = View.GONE
                binding.backButtonWorker.visibility = View.VISIBLE
            } else {
                // These are the main screens
                binding.bottomNavCardWorker.visibility = View.VISIBLE
                binding.backButtonWorker.visibility = View.GONE
            }
        }
    }
}