package com.rst.gadissalonmanagementsystemapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityRegisterBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.loginPromptText.setOnClickListener {
            // Finish this activity to go back to the Login screen
            finish()
        }

        binding.signupButton.setOnClickListener {
            validateAndRegister()
        }
    }

    private fun validateAndRegister() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

        // Clear previous errors
        binding.nameLayout.error = null
        binding.emailLayout.error = null
        binding.phoneLayout.error = null
        binding.passwordLayout.error = null
        binding.confirmPasswordLayout.error = null

        // Validation checks
        if (name.isEmpty()) {
            binding.nameLayout.error = "Name is required"
            return
        }
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email is required"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Please enter a valid email"
            return
        }
        if (phone.isEmpty()) {
            binding.phoneLayout.error = "Phone number is required"
            return
        }
        if (password.length < 6) {
            binding.passwordLayout.error = "Password must be at least 6 characters"
            return
        }
        if (password != confirmPassword) {
            binding.confirmPasswordLayout.error = "Passwords do not match"
            return
        }

        // If all checks pass, perform registration
        performRegistration(name, email, phone, password)
    }

    private fun performRegistration(name: String, email: String, phone: String, password: String) {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.signupButton.isEnabled = false

        // Use a coroutine to call our new suspend function
        lifecycleScope.launch {
            val result = FirebaseManager.registerUser(name, email, phone, password)

            // Hide the loading indicator
            binding.loadingIndicator.visibility = View.GONE
            binding.signupButton.isEnabled = true

            if (result.isSuccess) {
                // Registration was successful
                Toast.makeText(this@RegisterActivity, "Registration successful! Please log in.", Toast.LENGTH_LONG).show()
                finish() // Go back to the login screen
            } else {
                // Registration failed, show the error message
                val exception = result.exceptionOrNull()
                val errorMessage = exception?.message ?: "An unknown error occurred."
                binding.emailLayout.error = errorMessage
                Toast.makeText(this@RegisterActivity, "Registration Failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }
}