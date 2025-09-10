package com.rst.gadissalonmanagementsystemapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessaging
import com.rst.gadissalonmanagementsystemapp.databinding.ActivityLoginBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        auth = Firebase.auth

        // --- Configure Google Sign In ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // --- Initialize the Activity Result Launcher ---
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Log.w(TAG, "Google sign in failed", e)
                    Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Navigate to RegisterActivity screen
        binding.registerPromptText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Navigate to Forgot Password screen
        binding.forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.loginButton.setOnClickListener {
            Log.d(TAG, "Login button clicked.")
            validateAndLogin()
        }

        // Handle the login button click
        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        lifecycleScope.launch {
            try {
                auth.signInWithCredential(credential).await()

                // Sign-in success, now check if this is a new user in our database
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    // TODO: Here you would check your Firestore/Realtime Database
                    // to see if a user document with this UID already exists.
                    // If not, you create one. For now, we'll assume all are customers.

                    onLoginSuccess("CUSTOMER")
                }
            } catch (e: Exception) {
                Log.w(TAG, "signInWithCredential failed", e)
                Toast.makeText(this@LoginActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
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
        Log.d(TAG, "Validation passed. Performing Firebase login...")
        performFirebaseLogin(email, password)
    }

    private fun performFirebaseLogin(email: String, password: String) {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false

        lifecycleScope.launch {
            val result = FirebaseManager.loginUser(email, password)
            Log.d(TAG, "Login result received: $result")

            if (result.isSuccess) {
                val role = result.getOrNull() ?: "CUSTOMER"
                Log.d(TAG, "Login successful. Role: $role. Navigating...")
                onLoginSuccess(role)
            } else {
                val errorMessage = result.exceptionOrNull()?.message
                Log.w(TAG, "Login failed: $errorMessage")
                binding.loadingIndicator.visibility = View.GONE
                binding.loginButton.isEnabled = true
                Toast.makeText(this@LoginActivity, "Login Failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onLoginSuccess(role: String) {
        Log.d(TAG, "onLoginSuccess called. Saving role '$role' and starting new activity.")
        // Save the user's role to SharedPreferences
        val prefs = getSharedPreferences(Loading.PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putString(Loading.USER_ROLE_KEY, role.uppercase()).apply()

        // Navigate to the correct main activity
        val intent = when (role.uppercase()) {
            "ADMIN" -> Intent(this, AdminMainActivity::class.java)
            "WORKER" -> Intent(this, WorkerMainActivity::class.java)
            else -> Intent(this, CustomerMainActivity::class.java)
        }

        // After a successful login, get the latest FCM token and save it
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "FCM token for logged-in user: $token")
                MyFirebaseMessagingService().onNewToken(token)
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}