package com.rst.gadissalonmanagementsystemapp

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.functions
import kotlinx.coroutines.tasks.await
import java.lang.Exception

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
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!

            // Create the User object and explicitly set its 'id' to the uid from Firebase Auth
            val newUser = User(
                id = firebaseUser.uid, // Use the unique ID from Firebase Auth
                name = name,
                email = email,
                phone = phone
            )

            // --- ADDED LOG ---
            Log.d("FirebaseManager", "Registering new user in Firestore with ID: ${firebaseUser.uid}")
            usersCollection.document(firebaseUser.uid).set(newUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error during registration", e)
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

    // This is the simpler version that creates a user but logs the admin out.
    suspend fun createUserByAdmin(
        name: String, email: String, phone: String, password: String, role: String, imageUrl: String
    ): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!

            val newUser = User(
                id = firebaseUser.uid,
                name = name,
                email = email,
                phone = phone,
                role = role,
                imageUrl = imageUrl
            )
            usersCollection.document(firebaseUser.uid).set(newUser).await()
            Result.success(firebaseUser.uid) // Return the new UID
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // This function specifically calls the 'setUserRole' Cloud Function.
    suspend fun setRoleForUser(userId: String, role: String): Result<Unit> {
        return try {
            val data = hashMapOf("userId" to userId, "role" to role)
            Firebase.functions.getHttpsCallable("setUserRole").call(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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

    // Gets all confirmed bookings for a specific stylist on a specific date
    suspend fun getBookingsForStylistOnDate(stylistName: String, date: String): Result<List<AdminBooking>> {
        return try {
            val snapshots = firestore.collection("bookings")
                .whereEqualTo("stylistName", stylistName)
                .whereEqualTo("date", date)
                .whereEqualTo("status", "Confirmed")
                .get().await()
            val bookings = snapshots.toObjects(AdminBooking::class.java)
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Creates a new booking document in the 'bookings' collection
    suspend fun createBooking(booking: AdminBooking): Result<Unit> {
        return try {
            // --- THIS IS THE FIX ---
            // We convert the data class to a Map so we can add the server timestamp
            val dataToSave = mapOf(
                "hairstyleId" to booking.hairstyleId,
                "customerId" to booking.customerId,
                "stylistId" to booking.stylistId,
                "serviceName" to booking.serviceName,
                "customerName" to booking.customerName,
                "stylistName" to booking.stylistName,
                "date" to booking.date,
                "time" to booking.time,
                "status" to booking.status,
                "workerUnreadCount" to 0,
                "cancellationReason" to "",
                "declineReason" to "",
                "declinedBy" to emptyList<String>(),
                "timestamp" to System.currentTimeMillis() // <-- The critical part
            )
            // Save the Map, not the object
            firestore.collection("bookings").add(dataToSave).await()
            // --- END FIX ---
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetches the master list of available time slots from Firestore
    suspend fun getSalonTimeSlots(): Result<List<String>> {
        return try {
            val document = firestore.collection("app_content").document("salon_hours").get().await()
            // Get the 'time_slots' array field from the document
            val slots = document.get("time_slots") as? List<String> ?: emptyList()
            Result.success(slots)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FAVORITES FUNCTIONS ---
    // in FirebaseManager.kt

    fun addCurrentUserFavoritesListener(onUpdate: (List<Favoritable>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null

        return usersCollection.document(uid).collection("favorites")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("FavoritesListener", "Listener failed.", error)
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                val favoritesList = snapshots?.documents?.mapNotNull { doc ->

                    // --- 1. Handles NEW format ---
                    // (This is the one your app *tries* to write)
                    if (doc.contains("isVariantFavorite") && doc.getBoolean("isVariantFavorite") == true) {
                        Log.d("FavoritesListener", "Found new format favorite: ${doc.id}")
                        doc.toObject(FavoriteItem::class.java)?.copy(id = doc.id)

                        // --- 2. Handles Hairstyles ---
                    } else if (doc.contains("durationHours")) {
                        Log.d("FavoritesListener", "Found hairstyle favorite: ${doc.id}")
                        doc.toObject(Hairstyle::class.java)?.copy(id = doc.id)

                        // --- 3. NEW: Handles OLD format ---
                        // (This is the one it was ignoring)
                    } else if (doc.contains("variantFavorite") && doc.getBoolean("variantFavorite") == true) {
                        Log.d("FavoritesListener", "Found and parsing OLD format favorite: ${doc.id}")
                        try {
                            // Manually build the FavoriteItem from the old format
                            val name = doc.getString("name") ?: ""
                            val imageUrl = doc.getString("imageUrl") ?: ""
                            val originalId = doc.getString("originalId") ?: ""
                            val id = doc.id // Use the document ID

                            // Get the nested variant map
                            val variantMap = doc.get("favoritedVariant") as? Map<String, Any>
                            val variant = if (variantMap != null) {
                                ProductVariant(
                                    size = variantMap["size"] as? String ?: "",
                                    price = (variantMap["price"] as? Number)?.toDouble() ?: 0.0,
                                    priceOld = (variantMap["priceOld"] as? Number)?.toDouble(), // Can be null
                                    stock = (variantMap["stock"] as? Number)?.toInt() ?: 0
                                )
                            } else {
                                ProductVariant() // Empty placeholder
                            }

                            // Create the FavoriteItem object your adapter expects
                            FavoriteItem(
                                id = id,
                                name = name,
                                type = "PRODUCT",
                                originalId = originalId,
                                imageUrl = imageUrl,
                                isVariantFavorite = true, // Set to true so the object is valid
                                favoritedVariant = variant
                            )
                        } catch (e: Exception) {
                            Log.e("FavoritesListener", "Failed to parse old favorite format ${doc.id}", e)
                            null // If parsing fails, ignore it
                        }

                        // --- 4. Ignores everything else ---
                    } else {
                        Log.w(TAG, "Ignoring unknown favorite format. Data: ${doc.data.toString()}")
                        null
                    }
                } ?: emptyList()

                Log.d("FavoritesListener", "Listener triggered. Found ${favoritesList.size} valid favorite items.")
                onUpdate(favoritesList)
            }
    }

    fun addBookingsListener(onUpdate: (List<AdminBooking>) -> Unit): ListenerRegistration? {
        // We add a security check here. In a real app, you might also check
        // if the user is an admin on the client-side, but the Firestore rules provide the real security.
        val user = auth.currentUser
        if (user == null) {
            Log.w("FirebaseManager", "No user logged in, cannot listen for all bookings.")
            onUpdate(emptyList())
            return null
        }

        // We now correctly return the ListenerRegistration object.
        return firestore.collection("bookings")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "All bookings listener failed.", error)
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

    // --- CART & ORDER FUNCTIONS ---

    // Listens for real-time updates to the user's cart array
    fun addCurrentUserCartListener(onUpdate: (List<CartItem>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onUpdate(emptyList())
            return null
        }

        return usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Cart listener failed.", error)
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val cartData = snapshot.get("cart") as? List<Map<String, Any>>
                    val cartItems = cartData?.mapNotNull {
                        // We use the 'stock' saved in the cart doc
                        CartItem(
                            productId = it["productId"] as? String ?: "",
                            name = it["name"] as? String ?: "",
                            size = it["size"] as? String ?: "",
                            price = (it["price"] as? Number)?.toDouble() ?: 0.0,
                            quantity = (it["quantity"] as? Number)?.toInt() ?: 0,
                            imageUrl = it["imageUrl"] as? String ?: "",
                            stock = (it["stock"] as? Number)?.toInt() ?: 0 // --- GET STOCK ---
                        )
                    } ?: emptyList()
                    onUpdate(cartItems)
                } else {
                    onUpdate(emptyList())
                }
            }
    }

    // Adds a new item to the cart or updates the quantity of an existing one.
    suspend fun addOrUpdateCartItem(cartItem: CartItem): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val userDocRef = usersCollection.document(uid)
            val productDocRef = firestore.collection("products").document(cartItem.productId)

            firestore.runTransaction { transaction ->
                // 1. Get the product to check stock (outside transaction is fine, but good here)
                val productSnapshot = transaction.get(productDocRef)
                val product = productSnapshot.toObject(Product::class.java)
                val variant = product?.variants?.find { it.size == cartItem.size }
                val currentStock = variant?.stock ?: 0

                if (currentStock <= 0) {
                    throw Exception("This item is sold out.")
                }

                // 2. Get the user's cart
                val snapshot = transaction.get(userDocRef)
                val existingCartData = snapshot.get("cart") as? List<HashMap<String, Any>> ?: emptyList()
                val existingCart = existingCartData.map {
                    CartItem(
                        productId = it["productId"] as String,
                        name = it["name"] as String,
                        size = it["size"] as String,
                        price = (it["price"] as Number).toDouble(),
                        quantity = (it["quantity"] as Number).toInt(),
                        imageUrl = it["imageUrl"] as String,
                        stock = (it["stock"] as? Number)?.toInt() ?: 0 // Get existing stock
                    )
                }.toMutableList()

                val itemIndex = existingCart.indexOfFirst { it.productId == cartItem.productId && it.size == cartItem.size }

                if (itemIndex != -1) {
                    // Item exists, update quantity
                    val existingItem = existingCart[itemIndex]
                    val newQuantity = existingItem.quantity + cartItem.quantity
                    if (newQuantity > currentStock) {
                        throw Exception("Stock limit of $currentStock reached.")
                    }
                    // Update item with new quantity AND latest stock
                    existingCart[itemIndex] = existingItem.copy(quantity = newQuantity, stock = currentStock)
                } else {
                    // Item does not exist, add it (with stock info)
                    existingCart.add(cartItem.copy(stock = currentStock))
                }
                transaction.update(userDocRef, "cart", existingCart)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCart(cartItems: List<CartItem>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val userDocRef = usersCollection.document(uid)
            // Simply overwrite the "cart" field with the new local cart
            userDocRef.update("cart", cartItems).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cart", e)
            Result.failure(e)
        }
    }

    // Creates a final order and clears the cart
    suspend fun createProductOrder(order: ProductOrder): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {

            // Convert to a Map to save the correct timestamp
            val dataToSave = mapOf(
                "id" to order.id,
                "customerId" to order.customerId,
                "customerName" to order.customerName,
                "items" to order.items,
                "totalPrice" to order.totalPrice,
                "status" to order.status,
                "timestamp" to FieldValue.serverTimestamp() // <-- The critical part
                // paymentTimestamp will be null by default
            )

            firestore.runBatch { batch ->
                // 1. Save the order to a master 'product_orders' collection
                val orderRef = firestore.collection("product_orders").document(order.id)
                batch.set(orderRef, dataToSave) // Save the Map

                // 2. Clear the user's cart array
                val userRef = usersCollection.document(uid)
                batch.update(userRef, "cart", emptyList<CartItem>())
            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // Helper function to clear the cart
    private suspend fun clearCurrentUserCart(uid: String) {
        val cartItems = usersCollection.document(uid).collection("cart").get().await()
        for (document in cartItems.documents) {
            document.reference.delete().await()
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
            Log.d("FirebaseManager", "Fetching user document for UID: $uid")
            val document = usersCollection.document(uid).get().await()
            val user = document.toObject(User::class.java)
            // Manually set the ID to be the document's ID, which is always correct
            user?.id = document.id
            // --- ADDED LOG ---
            Log.d("FirebaseManager", "User fetched. ID from Firestore document is now: ${user?.id}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching user with UID: $uid", e)
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
            val data = hashMapOf("userId" to userId)
            Firebase.functions.getHttpsCallable("deleteUser").call(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addUserListener(onUpdate: (List<User>) -> Unit): ListenerRegistration?  {
        return usersCollection.addSnapshotListener { snapshots, error ->
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

    // Listens for real-time updates to the products collection
    fun addProductsListener(onUpdate: (List<Product>) -> Unit): ListenerRegistration? {
        val user = auth.currentUser
        if (user == null) {
            Log.w("FirebaseManager", "No user logged in, cannot fetch products.")
            onUpdate(emptyList())
            return null
        }

        // Return the ListenerRegistration object so the fragment can manage its lifecycle
        return firestore.collection("products")
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
            Log.d("FirebaseManager", "Sending support message and setting participants...")
            // Ensure the participants list is initialized with the sender's UID
            val ticketWithParticipants = message.copy(
                participantUids = listOf(message.senderUid)
            )
            firestore.collection("support_messages").add(ticketWithParticipants).await()
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

    /**
     * Deletes a product from Firestore and its associated image from Firebase Storage.
     */
    suspend fun deleteProduct(product: Product): Result<Unit> {
        return try {
            // First, delete the document from Firestore
            firestore.collection("products").document(product.id).delete().await()
            // Then, delete the associated image from Storage
            if (product.imageUrl.isNotBlank()) {
                storage.getReferenceFromUrl(product.imageUrl).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing product document in Firestore.
     */
    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            firestore.collection("products").document(product.id).set(product).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Adds a product to the current user's 'cart' subcollection
    suspend fun addToCart(product: Product, variant: ProductVariant): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return try {
            val cartDocRef = usersCollection.document(uid).collection("cart").document(product.id + "_" + variant.size)
            val productDocRef = firestore.collection("products").document(product.id)

            firestore.runTransaction { transaction ->
                val productSnapshot = transaction.get(productDocRef)
                val productData = productSnapshot.toObject(Product::class.java)
                val variantInDb = productData?.variants?.find { it.size == variant.size }
                val currentStock = variantInDb?.stock ?: 0

                val cartSnapshot = transaction.get(cartDocRef)
                val currentQuantityInCart = if (cartSnapshot.exists()) cartSnapshot.getLong("quantity")?.toInt() ?: 0 else 0

                if (currentQuantityInCart < currentStock) {
                    val newQuantity = currentQuantityInCart + 1
                    val cartItem = CartItem(
                        productId = product.id,
                        size = variant.size,
                        name = product.name,
                        price = variant.price,
                        quantity = newQuantity,
                        imageUrl = product.imageUrl
                    )
                    transaction.set(cartDocRef, cartItem)
                } else {
                    throw Exception("No more stock available for this item.")
                }
            }.await()
            Result.success(Unit)
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

    // --- NEW: Toggle favorite for a specific PRODUCT VARIANT ---
    suspend fun toggleFavoriteVariant(product: Product, variant: ProductVariant): Result<Boolean> {
        Log.d("FAVORITE_TEST", "--- EXECUTING NEW toggleFavoriteVariant ---")
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return try {
            // Create a unique ID for the favorite based on product and size
            val favoriteId = "${product.id}_${variant.size}"
            val favoriteDocRef = usersCollection.document(userId).collection("favorites").document(favoriteId)

            val document = favoriteDocRef.get().await()
            if (document.exists()) {
                // It's already a favorite, so remove it
                favoriteDocRef.delete().await()
                Result.success(false)
            } else {
                // It's not a favorite, so add it
                // Create the new FavoriteItem object that matches the website's format
                val newFavorite = FavoriteItem(
                    id = favoriteId,
                    originalId = product.id,
                    name = product.name,
                    imageUrl = product.imageUrl,
                    isVariantFavorite = true,
                    type = "PRODUCT",
                    favoritedVariant = variant
                )
                favoriteDocRef.set(newFavorite).await()
                Result.success(true)
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- NEW: Function to remove a favorite by its unique ID ---
    suspend fun unfavoriteItem(favoriteId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return try {
            val favoriteDocRef = usersCollection.document(userId).collection("favorites").document(favoriteId)
            favoriteDocRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
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

    suspend fun toggleFavorite(hairstyle: Hairstyle): Result<Boolean> {
        return toggleFavorite(hairstyle as Favoritable)
    }

    // Private helper for Hairstyles (and old Product logic)
    private suspend fun toggleFavorite(item: Favoritable): Result<Boolean> {
        Log.d("FAVORITE_TEST", "!!! EXECUTING OLD toggleFavorite(Product) !!!")
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return try {
            val favoriteDoc = usersCollection.document(userId).collection("favorites").document(item.id)
            val document = favoriteDoc.get().await()
            if (document.exists()) {
                favoriteDoc.delete().await()
                Result.success(false)
            } else {
                favoriteDoc.set(item).await()
                Result.success(true)
            }
        } catch (e: Exception) { Result.failure(e) }
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

    // This now correctly returns a ListenerRegistration?
    fun addCurrentUserSupportMessagesListener(onUpdate: (List<SupportMessage>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null

        return firestore.collection("support_messages")
            .whereArrayContains("participantUids", uid) // <-- THE FIX
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Current User Messages listener failed.", error)
                    return@addSnapshotListener
                }
                val messageList = snapshots?.map { doc ->
                    val message = doc.toObject(SupportMessage::class.java)
                    message.id = doc.id
                    message
                } ?: emptyList()
                onUpdate(messageList)
            }
    }





    /**
     * Deletes a hairstyle from Firestore and its associated image from Firebase Storage.
     */
    suspend fun deleteHairstyle(hairstyle: Hairstyle): Result<Unit> {
        return try {
            // First, delete the document from Firestore
            firestore.collection("hairstyles").document(hairstyle.id).delete().await()
            // Then, if it has an image, delete it from Storage
            if (hairstyle.imageUrl.isNotBlank()) {
                storage.getReferenceFromUrl(hairstyle.imageUrl).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Listens for real-time updates to the hairstyles collection.
     * @return A ListenerRegistration that can be removed to stop listening.
     */
    fun addHairstylesListener(onUpdate: (List<Hairstyle>) -> Unit): ListenerRegistration? {
        // We check if a user is logged in. This is a good practice for security,
        // although the rules currently allow anyone to read.
        val user = auth.currentUser
        if (user == null) {
            Log.w("FirebaseManager", "No user logged in, cannot listen for hairstyles.")
            onUpdate(emptyList())
            return null
        }

        // Return the ListenerRegistration object so the fragment can manage its lifecycle
        return firestore.collection("hairstyles")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Hairstyles listener failed.", error)
                    return@addSnapshotListener
                }

                // Map the Firestore documents to our Hairstyle data class,
                // ensuring the document ID is correctly assigned.
                val hairstyleList = snapshots?.map { doc ->
                    val hairstyle = doc.toObject(Hairstyle::class.java)
                    hairstyle.id = doc.id
                    hairstyle
                } ?: emptyList()

                // Send the updated list back to the fragment
                onUpdate(hairstyleList)
            }
    }

    // Updates the status of a specific support ticket
    suspend fun updateSupportTicketStatus(ticketId: String, newStatus: String): Result<Unit> {
        return try {
            firestore.collection("support_messages").document(ticketId)
                .update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates all unread replies in a support ticket to "Read".
     */
    suspend fun markSupportRepliesAsRead(ticketId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val repliesRef = firestore.collection("support_messages").document(ticketId).collection("replies")
            val query: Query = repliesRef.whereNotEqualTo("senderUid", uid).whereEqualTo("status", "SENT")

            // --- THIS IS THE FIX ---
            // 1. First, get the list of unread documents outside the transaction.
            val unreadDocsSnapshot = query.get().await()

            firestore.runTransaction { transaction ->
                // 2. Now, loop through the results you already fetched.
                for (document in unreadDocsSnapshot.documents) {
                    // 3. Perform only the 'update' operations inside the transaction.
                    transaction.update(document.reference, "status", "READ")
                }
                null // Transactions must return a result.
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error marking support replies as read", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing hairstyle document in Firestore.
     */
    suspend fun updateHairstyle(hairstyle: Hairstyle): Result<Unit> {
        return try {
            firestore.collection("hairstyles").document(hairstyle.id).set(hairstyle).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // This is the final, corrected version of the function.
    fun addWorkerScheduleListener(onUpdate: (List<AdminBooking>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.w("FirebaseManager", "Cannot get schedule, user is not logged in.")
            onUpdate(emptyList())
            return null // Return null if there's no user
        }

        // Return the ListenerRegistration object so the fragment can manage its lifecycle
        return firestore.collection("bookings")
            .whereEqualTo("stylistId", uid)
            .whereEqualTo("status", "Confirmed")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
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
                onUpdate(bookingList)
            }
    }
    suspend fun resetWorkerUnreadCount(bookingId: String): Result<Unit> {
        return try {
            firestore.collection("bookings").document(bookingId)
                .update("workerUnreadCount", 0).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    /**
     * Updates a booking's status to 'Declined' and adds a cancellation reason.
     * Used for directly assigned bookings.
     */
    suspend fun declineDirectBooking(bookingId: String, reason: String): Result<Unit> {
        return try {
            firestore.collection("bookings").document(bookingId)
                .update(mapOf(
                    "status" to "Declined",
                    "cancellationReason" to reason
                )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addDeclinedStylist(bookingId: String, stylistId: String): Result<Unit> {
        return try {
            firestore.collection("bookings").document(bookingId)
                .update("declinedBy", FieldValue.arrayUnion(stylistId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addPendingBookingsListener(onUpdate: (List<AdminBooking>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onUpdate(emptyList())
            return null
        }

        return firestore.collection("bookings")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    // ... (error handling)
                    return@addSnapshotListener
                }
                val bookingList = snapshots?.mapNotNull { doc ->
                    val booking = doc.toObject(AdminBooking::class.java)
                    booking.id = doc.id

                    // --- NEW FILTERING LOGIC ---
                    // Only include the booking if the current user has NOT declined it.
                    if (booking.declinedBy.contains(uid)) {
                        null // Exclude this booking
                    } else {
                        booking // Include this booking
                    }
                } ?: emptyList()
                onUpdate(bookingList)
            }
    }

    suspend fun acceptBooking(bookingId: String, stylist: User): Result<Unit> {
        return try {
            // --- THIS IS THE FIX ---
            // We convert to a Map to save the correct timestamp
            val updates = mapOf(
                "status" to "Confirmed",
                "stylistId" to stylist.id,
                "stylistName" to stylist.name,
                "timestamp" to System.currentTimeMillis() // <-- Add this line
            )
            firestore.collection("bookings").document(bookingId).update(updates).await()
            // --- END FIX ---
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

    /**
     * RENAMED and UPDATED
     * Listens for orders that are "Pending Pickup" OR "Ready for Pickup"
     */
    fun addWorkerOrdersListener(onUpdate: (List<ProductOrder>) -> Unit): ListenerRegistration? {
        if (auth.currentUser == null) {
            onUpdate(emptyList())
            return null
        }

        return firestore.collection("product_orders")
            // CHANGED: Use whereIn to get both statuses
            .whereIn("status", listOf("Pending Pickup", "Ready for Pickup"))
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Worker orders listener failed.", error)
                    return@addSnapshotListener
                }
                val orderList = snapshots?.map { doc ->
                    val order = doc.toObject(ProductOrder::class.java)
                    order.id = doc.id // Manually set the correct document ID
                    order
                } ?: emptyList()
                onUpdate(orderList)
            }
    }

    // Listens for new chat messages for a specific booking
    fun addChatMessagesListener(bookingId: String, onUpdate: (List<ChatMessage>) -> Unit): ListenerRegistration {
        return firestore.collection("bookings").document(bookingId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Chat listener failed.", error)
                    return@addSnapshotListener
                }
                val messages = snapshots?.toObjects(ChatMessage::class.java) ?: emptyList()
                onUpdate(messages)
            }
    }

    // Sends a new chat message
    suspend fun sendChatMessage(bookingId: String, message: ChatMessage): Result<Unit> {
        return try {
            // We no longer set a timestamp here; the server does it automatically.
            firestore.collection("bookings").document(bookingId).collection("messages")
                .add(message).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun addUnreadNotificationsListener(onUpdate: (Int) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onUpdate(0) // If no user, there are 0 unread notifications
            return null
        }

        // This query specifically targets the user's notifications subcollection
        // and filters for documents where isRead is false.
        return usersCollection.document(uid).collection("notifications")
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Unread notifications listener failed.", error)
                    onUpdate(0) // On error, report 0 unread
                    return@addSnapshotListener
                }

                // The size of the result is the count of unread notifications
                val unreadCount = snapshots?.size() ?: 0
                onUpdate(unreadCount)
            }
    }

    suspend fun updateProductOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        return try {
            firestore.collection("product_orders").document(orderId)
                .update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates a booking's status to 'Cancelled' and saves the reason.
     */
    suspend fun cancelBooking(bookingId: String, reason: String): Result<Unit> {
        return try {
            firestore.collection("bookings").document(bookingId)
                .update(mapOf(
                    "status" to "Cancelled",
                    "cancellationReason" to reason
                )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- CUSTOMER FUNCTIONS ---
    // Listens for real-time updates to the current user's bookings
    fun addCurrentUserBookingsListener(onUpdate: (List<AdminBooking>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null

        return firestore.collection("bookings")
            .whereEqualTo("customerId", uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Current user bookings listener failed.", error)
                    return@addSnapshotListener
                }
                // --- THIS IS THE FIX ---
                // We now manually map the documents to ensure the ID is set correctly.
                val bookingList = snapshots?.map { doc ->
                    val booking = doc.toObject(AdminBooking::class.java)
                    booking.id = doc.id // Manually set the correct document ID
                    booking
                } ?: emptyList()

                Log.d("FirebaseListener", "Listener triggered. Found ${bookingList.size} bookings for this user.")
                if (bookingList.isNotEmpty()) {
                    Log.d("FirebaseListener", "First booking status from Firestore: '${bookingList[0].status}'")
                }

                onUpdate(bookingList)
            }
    }

    // --- NOTIFICATION FUNCTIONS ---

    // Listens for real-time updates to the current user's notifications
    fun addUserNotificationsListener(onUpdate: (List<AppNotification>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onUpdate(emptyList())
            return null
        }

        return usersCollection.document(uid).collection("notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) { return@addSnapshotListener }
                val notifications = snapshots?.toObjects(AppNotification::class.java) ?: emptyList()
                onUpdate(notifications)
            }
    }

    // Marks a specific notification as read
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            usersCollection.document(uid).collection("notifications").document(notificationId)
                .update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // Marks all of a user's unread notifications as read
    suspend fun markAllNotificationsAsRead(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val notificationsRef = usersCollection.document(uid).collection("notifications")
            val query: Query = notificationsRef.whereEqualTo("isRead", false)

            // --- THIS IS THE FIX ---
            // 1. First, get the list of unread documents outside the transaction.
            val unreadSnapshot = query.get().await()

            firestore.runTransaction { transaction ->
                // 2. Now, loop through the results you already fetched.
                for (document in unreadSnapshot.documents) {
                    // 3. Perform only the 'update' operations inside the transaction.
                    transaction.update(document.reference, "isRead", true)
                }
                null // Transactions must return a result.
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error marking notifications as read", e)
            Result.failure(e)
        }
    }

    // Listens for real-time updates to the current user's product orders
    fun addCurrentUserOrdersListener(onUpdate: (List<ProductOrder>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null
        val userName = auth.currentUser?.displayName ?: ""

        return firestore.collection("product_orders")
            .whereEqualTo("customerId", uid)  // Filter by name
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) { return@addSnapshotListener }
                val orderList = snapshots?.map { doc ->
                    val order = doc.toObject(ProductOrder::class.java)
                    order.id = doc.id // Manually set the correct document ID
                    order
                } ?: emptyList()
                onUpdate(orderList)
            }
    }

    // This function handles both new and returning Google users
    suspend fun signInWithGoogle(firebaseUser: com.google.firebase.auth.FirebaseUser): Result<String> {
        return try {
            val userDocRef = usersCollection.document(firebaseUser.uid)
            val document = userDocRef.get().await()

            if (document.exists()) {
                // Case 1: The user already exists in Firestore.
                // We just fetch their role and return it.
                Log.d("FirebaseManager", "Google user already exists. Fetching role.")
                val role = document.getString("role") ?: "CUSTOMER"
                Result.success(role)
            } else {
                // Case 2: This is a new user signing in with Google for the first time.
                // We create a new profile for them.
                Log.d("FirebaseManager", "New Google user. Creating profile in Firestore.")
                val newUser = User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "No Name",
                    email = firebaseUser.email ?: "",
                    phone = firebaseUser.phoneNumber ?: "", // Usually empty
                    imageUrl = firebaseUser.photoUrl?.toString() ?: "",
                    role = "CUSTOMER" // All new Google sign-ups are customers
                )
                userDocRef.set(newUser).await()
                Result.success("CUSTOMER")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sends a password reset link to the provided email address
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetches a specific list of users (stylists) by their unique IDs
    suspend fun getStylistsByIds(stylistIds: List<String>): Result<List<User>> {
        if (stylistIds.isEmpty()) {
            return Result.success(emptyList())
        }
        return try {
            val documentSnapshots = usersCollection.whereIn("id", stylistIds).get().await()
            val userList = documentSnapshots.toObjects(User::class.java)
            Result.success(userList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Updates all unread messages in a chat to "Read"
    // This is your corrected code - it's perfect!
    suspend fun markMessagesAsRead(bookingId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val messagesRef = firestore.collection("bookings").document(bookingId).collection("messages")
            val query: Query = messagesRef.whereNotEqualTo("senderUid", uid).whereEqualTo("status", "SENT")

            // 1. You correctly fetch the list of documents FIRST, outside the transaction.
            val unreadDocsSnapshot = query.get().await()

            firestore.runTransaction { transaction ->
                // 2. Now, inside the transaction, you loop through the results you already have.
                for (document in unreadDocsSnapshot) {
                    // 3. You perform only WRITE operations inside the transaction, which is the best practice.
                    transaction.update(document.reference, "status", "READ")
                }
                null // Transactions must return a result; null is fine.
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error marking messages as read", e)
            Result.failure(e)
        }
    }

    // Calls the new Cloud Function to get available slots
    suspend fun getAvailableSlots(stylistName: String, date: String, hairstyleId: String): Result<List<String>> {
        return try {
            val data = hashMapOf(
                "stylistName" to stylistName,
                "date" to date,
                "hairstyleId" to hairstyleId
            )
            val result = Firebase.functions.getHttpsCallable("getAvailableSlots").call(data).await()
            val slots = (result.data as? Map<*, *>)?.get("slots") as? List<String> ?: emptyList()
            Result.success(slots)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- WORKER NOTIFICATION FUNCTIONS ---

    /**
     * Listens for the COUNT of pending booking requests.
     * This is more efficient than fetching the whole list for a badge.
     */
    fun addPendingBookingsCountListener(onUpdate: (Int) -> Unit): ListenerRegistration? {
        // Only workers and admins should be able to see this.
        if (auth.currentUser == null) return null

        return firestore.collection("bookings")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onUpdate(0)
                    return@addSnapshotListener
                }
                onUpdate(snapshots?.size() ?: 0)
            }
    }

    /**
     * Listens for the COUNT of pending product orders.
     */
    fun addPendingOrdersCountListener(onUpdate: (Int) -> Unit): ListenerRegistration? {
        if (auth.currentUser == null) return null

        return firestore.collection("product_orders")
            .whereEqualTo("status", "Pending Pickup")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onUpdate(0)
                    return@addSnapshotListener
                }
                onUpdate(snapshots?.size() ?: 0)
            }
    }

    /**
     * Listens for real-time updates to the entire 'product_orders' collection.
     * Intended for admin use.
     * @return A ListenerRegistration that can be removed to stop listening.
     */
    fun addAllProductOrdersListener(onUpdate: (List<ProductOrder>) -> Unit): ListenerRegistration? {
        // We add a security check here. In a real app, you might also check
        // if the user is an admin on the client-side, but the Firestore rules provide the real security.
        if (auth.currentUser == null) {
            Log.w("FirebaseManager", "No user logged in, cannot fetch all orders.")
            onUpdate(emptyList())
            return null
        }

        return firestore.collection("product_orders")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "All orders listener failed.", error)
                    return@addSnapshotListener
                }

                // Map the Firestore documents to our ProductOrder data class.
                // We don't need to manually set the ID here since we create it in the app.
                val orderList = snapshots?.map { doc ->
                    val order = doc.toObject(ProductOrder::class.java)
                    order.id = doc.id // Manually set the correct document ID
                    order
                } ?: emptyList()

                // Send the updated list back to the fragment
                onUpdate(orderList)
            }
    }

    // Listens for real-time updates to the reply thread of a support ticket
    fun addSupportTicketRepliesListener(ticketId: String, onUpdate: (List<ChatMessage>) -> Unit): ListenerRegistration {
        return firestore.collection("support_messages").document(ticketId).collection("replies")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null) { return@addSnapshotListener }
                val messages = snapshots?.toObjects(ChatMessage::class.java) ?: emptyList()
                onUpdate(messages)
            }
    }

    // Sends a new reply to a support ticket
    suspend fun sendSupportReply(ticketId: String, reply: ChatMessage, allParticipantIds: List<String>): Result<Unit> {
        return try {
            val db = Firebase.firestore
            val ticketRef = db.collection("support_messages").document(ticketId)
            val repliesRef = ticketRef.collection("replies")

            // Use a batch write to perform both actions atomically (all or nothing)
            db.runBatch { batch ->
                // 1. Add the new reply message to the subcollection
                batch.set(repliesRef.document(), reply)

                // 2. Update the parent ticket to include all participants
                // FieldValue.arrayUnion safely adds items without creating duplicates.
                batch.update(ticketRef, "participantUids", FieldValue.arrayUnion(*allParticipantIds.toTypedArray()))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error sending reply and updating participants", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches a single booking document by its unique ID.
     */
    suspend fun getBooking(bookingId: String): Result<AdminBooking?> {
        return try {
            val document = firestore.collection("bookings").document(bookingId).get().await()
            val booking = document.toObject(AdminBooking::class.java)
            // Manually set the ID, as it's the document's path
            booking?.id = document.id
            Result.success(booking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches a single product order document by its unique ID.
     */
    suspend fun getProductOrder(orderId: String): Result<ProductOrder?> {
        return try {
            val document = firestore.collection("product_orders").document(orderId).get().await()
            val order = document.toObject(ProductOrder::class.java)
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Listens for the total count of unread chat messages for the current worker
     * across all their confirmed bookings.
     */
    fun addWorkerUnreadMessageListener(onUpdate: (Int) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null

        return firestore.collection("bookings")
            .whereEqualTo("stylistId", uid)
            .whereEqualTo("status", "Confirmed")
            .whereGreaterThan("workerUnreadCount", 0) // Only get bookings that HAVE unread messages
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onUpdate(0)
                    return@addSnapshotListener
                }
                // The size of the result is the number of conversations with unread messages.
                onUpdate(snapshots?.size() ?: 0)
            }
    }

    /**
     * Updates the FCM (Firebase Cloud Messaging) token for a specific user.
     * This is crucial for sending push notifications.
     */
    suspend fun updateUserFCMToken(uid: String, token: String): Result<Unit> {
        return try {
            // Find the user's document by their UID and update the 'fcmToken' field.
            usersCollection.document(uid).update("fcmToken", token).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error updating FCM token", e)
            Result.failure(e)
        }
    }
}