package com.rst.gadissalonmanagementsystemapp

import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.storage

object FirebaseManager {

    private const val TAG = "FirebaseManager"
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
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
        Log.d(TAG, "Attempting to login user: $email")
        return try {
            // Step 1: Sign in the user with Firebase Authentication
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            Log.d(TAG, "Firebase Auth sign-in successful. UID: ${firebaseUser?.uid}")

            if (firebaseUser != null) {
                // Step 2: Get the user's document from Firestore
                Log.d(TAG, "Fetching user document from Firestore...")
                val userDocument = usersCollection.document(firebaseUser.uid).get().await()
                Log.d(TAG, "Firestore document fetched successfully.")

                // Step 3: Get the 'role' field from the document
                val role = userDocument.getString("role") ?: "CUSTOMER"
                Log.d(TAG, "User role found: $role")

                Result.success(role)
            } else {
                Log.w(TAG, "Firebase Auth successful but user object is null.")
                Result.failure(Exception("Failed to sign in."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed with exception: ${e.message}")
            Result.failure(e)
        }
    }

    // This function lets an admin create a user with a specific role
    suspend fun createUserByAdmin(name: String, email: String, phone: String, password: String, role: String, imageUrl: String): Result<Unit> {
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
                    role = role,
                    imageUrl = imageUrl// Use the role provided by the admin
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

    // Function to upload an image and get its download URL
    suspend fun uploadImage(uri: Uri, folder: String, fileName: String): Result<Uri> {
        return try {
            val storageRef = storage.reference.child("$folder/$fileName")
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addProduct(product: Product): Result<Unit> {
        return try {
            firestore.collection("products").document(product.id).set(product).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Saves a new hairstyle object to the "hairstyles" collection
    suspend fun addHairstyle(hairstyle: Hairstyle): Result<Unit> {
        return try {
            firestore.collection("hairstyles").document(hairstyle.id).set(hairstyle).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            Log.d("FirebaseManager", "Fetching all products from Firestore...")
            val documents = firestore.collection("products").get().await()
            val productList = documents.toObjects(Product::class.java)
            Log.d("FirebaseManager", "Successfully fetched ${productList.size} products.")
            Result.success(productList)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching products", e)
            Result.failure(e)
        }
    }

    suspend fun getAllHairstyles(): Result<List<Hairstyle>> {
        return try {
            Log.d("FirebaseManager", "Fetching all hairstyles from Firestore...")
            val documents = firestore.collection("hairstyles").get().await()
            val hairstyleList = documents.toObjects(Hairstyle::class.java)
            Log.d("FirebaseManager", "Successfully fetched ${hairstyleList.size} hairstyles.")
            Result.success(hairstyleList)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching hairstyles", e)
            Result.failure(e)
        }
    }

    // --- ADD THIS FUNCTION to get a single user's details ---
    suspend fun getUser(uid: String): Result<User?> {
        return try {
            val document = usersCollection.document(uid).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ADD THIS FUNCTION to update a user's image URL ---
    suspend fun updateUserProfileImage(uid: String, imageUrl: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update("imageUrl", imageUrl).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetches the 'about_us' document from the 'app_content' collection
    suspend fun getAboutUsContent(): Result<Map<String, String>> {
        return try {
            Log.d("FirebaseManager", "Fetching About Us content...")
            val document = firestore.collection("app_content").document("about_us").get().await()
            val contentMap = mapOf(
                "salon_about" to (document.getString("salon_about") ?: ""),
                "app_about" to (document.getString("app_about") ?: "")
            )
            Result.success(contentMap)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching About Us content", e)
            Result.failure(e)
        }
    }

    // Updates the 'about_us' document
    suspend fun updateAboutUsContent(salonAbout: String, appAbout: String): Result<Unit> {
        return try {
            Log.d("FirebaseManager", "Updating About Us content...")
            val content = mapOf("salon_about" to salonAbout, "app_about" to appAbout)
            firestore.collection("app_content").document("about_us").set(content).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error updating About Us content", e)
            Result.failure(e)
        }
    }

    // Saves a new support message to the 'support_messages' collection
    suspend fun sendSupportMessage(message: SupportMessage): Result<Unit> {
        return try {
            Log.d("FirebaseManager", "Sending support message...")
            firestore.collection("support_messages").add(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error sending support message", e)
            Result.failure(e)
        }
    }

    // Fetches the 'location' document from the 'app_content' collection
    suspend fun getSalonLocation(): Result<SalonLocation> {
        return try {
            val document = firestore.collection("app_content").document("location").get().await()
            val location = document.toObject(SalonLocation::class.java) ?: SalonLocation()
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Updates the 'location' document
    suspend fun updateSalonLocation(location: SalonLocation): Result<Unit> {
        return try {
            firestore.collection("app_content").document("location").set(location).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllSupportMessages(): Result<List<SupportMessage>> {
        return try {
            val documents = firestore.collection("support_messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            val messages = documents.toObjects(SupportMessage::class.java)
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSupportMessageStatus(messageId: String, newStatus: String): Result<Unit> {
        return try {
            firestore.collection("support_messages").document(messageId)
                .update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSupportMessage(messageId: String): Result<Unit> {
        return try {
            firestore.collection("support_messages").document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}