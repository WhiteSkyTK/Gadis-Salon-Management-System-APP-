package com.rst.gadissalonmanagementsystemapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.view.ViewCompat
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityLoginBinding
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Navigate to Register screen
        binding.registerPromptText.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        // Navigate to Forgot Password screen
        binding.forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Handle the login button click
        binding.loginButton.setOnClickListener {
            validateAndLogin()
        }
    }

    private fun validateAndLogin() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        // Clear previous errors
        binding.emailLayout.error = null
        binding.passwordLayout.error = null

        // --- Validation ---
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email is required"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Please enter a valid email"
            return
        }

        if (password.isEmpty()) {
            binding.passwordLayout.error = "Password is required"
            return
        }

        // If all validation passes, perform the login
        performLogin(email)
    }

    private fun performLogin(email: String) {
        // Show loading state
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false

        // Use a coroutine to simulate a network delay
        lifecycleScope.launch {
            delay(2000) // Fake 2-second delay

            // --- Dummy Role Logic ---
            // In a real app, you would get the role from your backend API
            val role = when {
                email.startsWith("admin@", true) -> "ADMIN"
                email.startsWith("worker@", true) -> "WORKER"
                else -> "CUSTOMER"
            }

            onLoginSuccess(role)
        }
    }

    private fun onLoginSuccess(role: String) {
        // Save the user's role to SharedPreferences
        val prefs = getSharedPreferences(Loading.PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putString(Loading.USER_ROLE_KEY, role).apply()

        // Navigate to the correct main activity
        val intent = when (role) {
            "ADMIN" -> Intent(this, AdminMainActivity::class.java)
            "WORKER" -> Intent(this, WorkerMainActivity::class.java)
            else -> Intent(this, CustomerMainActivity::class.java)
        }

        // Add flags to clear the back stack so the user can't go back to the login screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finish LoginActivity
    }
}