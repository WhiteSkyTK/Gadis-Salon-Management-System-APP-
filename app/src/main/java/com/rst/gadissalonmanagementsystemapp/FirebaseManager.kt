package com.rst.gadissalonmanagementsystemapp

import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.lang.Exception


object FirebaseManager {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val usersCollection = firestore.collection("users")

    // This function will create a user and save their details.
    // It returns a Result object: Success if it works, Failure with an error message if it doesn't.
    suspend fun registerUser(name: String, email: String, phone: String, password: String): Result<Unit> {
        return try {
            // Step 1: Create the user in Firebase Authentication
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Step 2: Create a User object with the details
                val newUser = User(
                    id = firebaseUser.uid, // Use the unique ID from Firebase Auth
                    name = name,
                    email = email,
                    phone = phone,
                )

                // Step 3: Save the User object to the "users" collection in Firestore
                usersCollection.document(firebaseUser.uid).set(newUser).await()
                Result.success(Unit) // Return success
            } else {
                Result.failure(Exception("Failed to create user account."))
            }
        } catch (e: Exception) {
            // Return failure with the specific error message from Firebase
            Result.failure(e)
        }
    }

    // NEW LOGIN FUNCTION
    suspend fun loginUser(email: String, password: String): Result<String> {
        return try {
            // Step 1: Sign in the user with Firebase Authentication
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Step 2: Get the user's document from Firestore using their unique ID (uid)
                val userDocument = usersCollection.document(firebaseUser.uid).get().await()

                // Step 3: Get the 'role' field from the document
                val role = userDocument.getString("role") ?: "CUSTOMER" // Default to CUSTOMER if no role is found

                Result.success(role)
            } else {
                Result.failure(Exception("Failed to sign in."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // This function lets an admin create a user with a specific role
    suspend fun createUserByAdmin(name: String, email: String, phone: String, password: String, role: String): Result<Unit> {
        return try {
            // We create the user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // We create the User object, passing in the specified role
                val newUser = User(
                    id = firebaseUser.uid,
                    name = name,
                    email = email,
                    phone = phone,
                    role = role // Use the role provided by the admin
                )
                // And save it to Firestore
                usersCollection.document(firebaseUser.uid).set(newUser).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to create user account."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val documentSnapshots = usersCollection.get().await()
            // Convert the documents from Firestore into a list of our User data class
            val userList = documentSnapshots.toObjects(User::class.java)
            Result.success(userList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllBookings(): Result<List<AdminBooking>> {
        return try {
            val documentSnapshots = firestore.collection("bookings").get().await()
            val bookingList = documentSnapshots.toObjects(AdminBooking::class.java)
            Result.success(bookingList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}