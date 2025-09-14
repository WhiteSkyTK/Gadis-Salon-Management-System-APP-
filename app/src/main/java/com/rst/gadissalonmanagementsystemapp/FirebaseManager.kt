package com.rst.gadissalonmanagementsystemapp

import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.storage
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.app

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
                val newUser = User(name = name, email = email, phone = phone)

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
    // This function now returns the new user's UID on success
    suspend fun createUserByAdmin(
        context: Context, // We need context to initialize the second app
        name: String, email: String, phone: String, password: String, role: String, imageUrl: String
    ): Result<String> {
        // Create a unique name for our temporary app instance
        val tempAppName = "AdminCreateUser"
        var tempAuth = auth // Start with the current auth
        var tempApp: FirebaseApp? = null

        try {
            // Initialize a temporary, secondary Firebase app
            tempApp = FirebaseApp.initializeApp(context, Firebase.app.options, tempAppName)
            tempAuth = Firebase.auth(tempApp)

            // Step 1: Create the new user using the temporary auth instance
            val authResult = tempAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Step 2: Save the user's details to Firestore
                val newUser = User(
                    id = firebaseUser.uid,
                    name = name,
                    email = email,
                    phone = phone,
                    role = role,
                    imageUrl = imageUrl
                )
                usersCollection.document(firebaseUser.uid).set(newUser).await()
                return Result.success(firebaseUser.uid) // Return the new UID
            } else {
                return Result.failure(Exception("Failed to create user account."))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            // Step 3: IMPORTANT - Delete the temporary app instance to clean up
            FirebaseApp.getInstance(tempAppName).delete()
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val documentSnapshots = usersCollection.get().await()
            // Convert the documents from Firestore into a list of our User data class
            val userList = documentSnapshots.map { document ->
                // Convert the document to a User object.
                val user = document.toObject(User::class.java)
                // Manually set the user's ID to be the document's ID.
                user.id = document.id
                user
            }
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

    suspend fun updateUserProfile(uid: String, name: String, phone: String, imageUrl: String): Result<Unit> {
        return try {
            val userUpdates = mapOf(
                "name" to name,
                "phone" to phone,
                "imageUrl" to imageUrl // Add the imageUrl to the update
            )
            usersCollection.document(uid).update(userUpdates).await()
            Result.success(Unit)
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

    // Updates a user's document in Firestore
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Deletes a user's document from Firestore
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addUserListener(onUpdate: (List<User>) -> Unit) {
        usersCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w("FirebaseManager", "User listener failed.", error)
                return@addSnapshotListener
            }

            val userList = snapshots?.map { document ->
                val user = document.toObject(User::class.java)
                user.id = document.id
                user
            } ?: emptyList()

            onUpdate(userList)
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
            Log.d("FirebaseManager", "Fetching all support messages...")
            val documentSnapshots = firestore.collection("support_messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            val messages = documentSnapshots.map { document ->
                val message = document.toObject(SupportMessage::class.java)
                message.id = document.id // Manually set the correct ID
                message
            }

            Log.d("FirebaseManager", "Successfully fetched ${messages.size} messages.")
            Result.success(messages)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching support messages", e)
            Result.failure(e)
        }
    }

    suspend fun updateSupportMessageStatus(messageId: String, newStatus: String): Result<Unit> {
        return try {
            firestore.collection("support_messages").document(messageId)
                .update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Add a log to see the specific error
            Log.e("FirebaseManager", "Error updating message status", e)
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

    // Adds a product to the current user's 'cart' subcollection
    suspend fun addToCart(product: Product, variant: ProductVariant): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return try {
            val cartCollection = usersCollection.document(userId).collection("cart")
            val cartItem = CartItem(
                name = product.name,
                price = variant.price,
                quantity = 1,
                imageUrl = product.imageUrl
            )
            cartCollection.document(product.id).set(cartItem).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Toggles a product in the current user's 'favorites' subcollection
    suspend fun toggleFavorite(product: Product): Result<Boolean> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return try {
            val favoriteDoc = usersCollection.document(userId).collection("favorites").document(product.id)
            val document = favoriteDoc.get().await()

            if (document.exists()) {
                favoriteDoc.delete().await()
                Result.success(false) // No longer a favorite
            } else {
                favoriteDoc.set(product).await()
                Result.success(true) // Is now a favorite
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addFaqListener(onUpdate: (List<FaqItem>) -> Unit) {
        firestore.collection("faqs")
            .addSnapshotListener { snapshots, error ->
                if (error != null) { return@addSnapshotListener }
                val faqList = snapshots?.map { doc ->
                    val faq = doc.toObject(FaqItem::class.java)
                    faq.id = doc.id
                    faq
                } ?: emptyList()
                onUpdate(faqList)
            }
    }

    suspend fun addFaq(faq: FaqItem): Result<Unit> {
        return try {
            firestore.collection("faqs").add(faq).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFaq(faqId: String): Result<Unit> {
        return try {
            firestore.collection("faqs").document(faqId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Checks if a product is in the user's favorites
    suspend fun isFavorite(productId: String): Result<Boolean> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return try {
            val document = usersCollection.document(userId).collection("favorites").document(productId).get().await()
            Result.success(document.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- WORKER FUNCTIONS ---

    suspend fun getCurrentUser(): Result<User?> {
        val uid = auth.currentUser?.uid ?: return Result.success(null)
        return try {
            val document = usersCollection.document(uid).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBooking(bookingId: String): Result<Unit> {
        return try {
            firestore.collection("bookings").document(bookingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addCurrentUserSupportMessagesListener(onUpdate: (List<SupportMessage>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onUpdate(emptyList())

        firestore.collection("support_messages")
            .whereEqualTo("senderUid", uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Current User Messages listener failed.", error)
                    return@addSnapshotListener
                }
                // Map the Firestore documents to a list of SupportMessage objects
                val messageList = snapshots?.map { doc ->
                    val message = doc.toObject(SupportMessage::class.java)
                    message.id = doc.id // Manually set the correct document ID
                    message
                } ?: emptyList()
                onUpdate(messageList)
            }
    }



    // Listens for real-time updates to the products collection
    fun addProductsListener(onUpdate: (List<Product>) -> Unit) {
        firestore.collection("products")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Products listener failed.", error)
                    return@addSnapshotListener
                }

                val productList = snapshots?.map { doc ->
                    val product = doc.toObject(Product::class.java)
                    product.id = doc.id
                    product
                } ?: emptyList()
                onUpdate(productList)
            }
    }

    fun addWorkerScheduleListener(onUpdate: (List<AdminBooking>) -> Unit) {
        // First, get the currently logged-in user's name to filter the bookings
        val currentUserName = auth.currentUser?.displayName
        if (currentUserName == null) {
            Log.w("FirebaseManager", "Cannot get schedule, user is not logged in or has no display name.")
            onUpdate(emptyList()) // Return an empty list if there's no user
            return
        }

        firestore.collection("bookings")
            // Find documents where the 'stylistName' field matches the current worker's name
            .whereEqualTo("stylistName", currentUserName)
            // And where the 'status' field is 'Confirmed'
            .whereEqualTo("status", "Confirmed")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Worker Schedule listener failed.", error)
                    return@addSnapshotListener
                }

                val bookingList = snapshots?.map { doc ->
                    val booking = doc.toObject(AdminBooking::class.java)
                    booking.id = doc.id
                    booking
                } ?: emptyList()

                // Send the updated list back to the fragment
                onUpdate(bookingList)
            }
    }

    fun addPendingBookingsListener(onUpdate: (List<AdminBooking>) -> Unit) {
        firestore.collection("bookings")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Pending Bookings listener failed.", error)
                    return@addSnapshotListener
                }
                val bookingList = snapshots?.map { doc ->
                    val booking = doc.toObject(AdminBooking::class.java)
                    booking.id = doc.id
                    booking
                } ?: emptyList()
                onUpdate(bookingList)
            }
    }

    suspend fun acceptBooking(bookingId: String, stylist: User): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "Confirmed",
                "stylistName" to stylist.name
            )
            firestore.collection("bookings").document(bookingId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // A general function to update status, useful for declining/cancelling
    suspend fun updateBookingStatus(bookingId: String, newStatus: String): Result<Unit> {
        return try {
            firestore.collection("bookings").document(bookingId).update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addPendingOrdersListener(onUpdate: (List<ProductOrder>) -> Unit) {
        firestore.collection("product_orders")
            .whereEqualTo("status", "Pending Pickup")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) { return@addSnapshotListener }
                val orderList = snapshots?.toObjects(ProductOrder::class.java) ?: emptyList()
                onUpdate(orderList)
            }
    }

    // Listens for new chat messages for a specific booking
    fun addChatMessagesListener(bookingId: String, onUpdate: (List<ChatMessage>) -> Unit) {
        firestore.collection("bookings").document(bookingId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, _ ->
                val messages = snapshots?.toObjects(ChatMessage::class.java) ?: emptyList()
                onUpdate(messages)
            }
    }

    // Sends a new chat message
    suspend fun sendChatMessage(bookingId: String, message: ChatMessage): Result<Unit> {
        return try {
            firestore.collection("bookings").document(bookingId).collection("messages")
                .add(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




    // --- CUSTOMER FUNCTIONS ---

    // Listens for real-time updates to the current user's bookings
    fun addCurrentUserBookingsListener(onUpdate: (List<AdminBooking>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onUpdate(emptyList()) // If no one is logged in, return an empty list
            return
        }

        firestore.collection("bookings")
            .whereEqualTo("customerName", currentUser.displayName) // Filter by the user's name
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Current User Bookings listener failed.", error)
                    return@addSnapshotListener
                }
                val bookingList = snapshots?.map { doc ->
                    val booking = doc.toObject(AdminBooking::class.java)
                    booking.id = doc.id
                    booking
                } ?: emptyList()
                onUpdate(bookingList)
            }
    }
}