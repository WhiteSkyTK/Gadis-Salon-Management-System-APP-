package com.rst.gadissalonmanagementsystemapp

import android.os.Bundle
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
    }
}