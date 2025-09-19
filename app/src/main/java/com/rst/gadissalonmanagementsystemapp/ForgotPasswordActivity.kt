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
        binding.sendLinkButton.setOnClickListener {
            validateAndSend()
        }
        binding.backToLoginText.setOnClickListener {
            finish()
        }
    }

    private fun validateAndSend() {
        val email = binding.emailInput.text.toString().trim()
        binding.emailLayout.error = null

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Please enter a valid email"
            return
        }

        // If validation passes, call the function to send the real reset link
        performSendResetLink(email)
    }

    private fun performSendResetLink(email: String) {
        // Show loading state
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.sendLinkButton.isEnabled = false

        // Use a coroutine to call our suspend function in FirebaseManager
        lifecycleScope.launch {
            val result = FirebaseManager.sendPasswordResetEmail(email)

            // Hide loading state
            binding.loadingIndicator.visibility = View.GONE
            binding.sendLinkButton.isEnabled = true

            if (result.isSuccess) {
                // Show success message
                Toast.makeText(this@ForgotPasswordActivity, "Reset link sent successfully to your email.", Toast.LENGTH_LONG).show()
                // Go back to the login screen automatically after success
                finish()
            } else {
                // Show the specific error from Firebase
                val errorMessage = result.exceptionOrNull()?.message ?: "An unknown error occurred."
                Toast.makeText(this@ForgotPasswordActivity, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }
}