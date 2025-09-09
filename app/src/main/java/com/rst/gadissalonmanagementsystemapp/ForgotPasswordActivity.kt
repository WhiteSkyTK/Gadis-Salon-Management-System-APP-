package com.rst.gadissalonmanagementsystemapp

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgot_password)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Handle the "Send Reset Link" button click
        binding.sendLinkButton.setOnClickListener {
            validateAndSend()
        }

        // Handle the "Back to LoginActivity" text click
        binding.backToLoginText.setOnClickListener {
            finish() // Simply closes this activity and returns to the previous one (LoginActivity)
        }
    }

    private fun validateAndSend() {
        val email = binding.emailInput.text.toString().trim()
        binding.emailLayout.error = null

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Please enter a valid email"
            return
        }

        // If validation passes, simulate sending the link
        performSendResetLink()
    }

    private fun performSendResetLink() {
        // Show loading state
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.sendLinkButton.isEnabled = false

        // Use a coroutine to simulate a network delay
        lifecycleScope.launch {
            delay(2000) // Fake 2-second delay

            // Hide loading state
            binding.loadingIndicator.visibility = View.GONE
            binding.sendLinkButton.isEnabled = true

            // Show success message
            Toast.makeText(this@ForgotPasswordActivity, "Reset link sent to your email", Toast.LENGTH_LONG).show()

            // Go back to the login screen automatically after success
            finish()
        }
    }
}