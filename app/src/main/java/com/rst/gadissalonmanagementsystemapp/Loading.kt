package com.rst.gadissalonmanagementsystemapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Loading : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "GadisSalonPrefs"
        const val IS_FIRST_TIME_KEY = "IsFirstTime"
        const val USER_ROLE_KEY = "UserRole"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_loading)

        // Use a Handler to create a delay
        Handler(Looper.getMainLooper()).postDelayed({

            /*
            // Access SharedPreferences
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val isFirstTime = prefs.getBoolean(IS_FIRST_TIME_KEY, true)

            if (isFirstTime) {
                // This is the first time the app is opened
                // Set the flag to false for future launches
                prefs.edit().putBoolean(IS_FIRST_TIME_KEY, false).apply()

                // Go to LoginActivity screen
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                // Not the first time, check if user is logged in by checking their role
                val userRole = prefs.getString(USER_ROLE_KEY, null)

                when (userRole) {
                    "ADMIN" -> startActivity(Intent(this, AdminMainActivity::class.java))
                    "WORKER" -> startActivity(Intent(this, WorkerMainActivity::class.java))
                    "CUSTOMER" -> startActivity(Intent(this, CustomerMainActivity::class.java))
                    else -> {
                        // No role saved, so user is not logged in
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                }
            }

             */

            startActivity(Intent(this, AdminMainActivity::class.java))

            // Finish the Loading activity so the user can't navigate back to it
            finish()

        }, 2000)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.load)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}