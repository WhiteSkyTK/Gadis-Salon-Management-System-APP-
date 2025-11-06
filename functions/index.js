const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { defineString } = require("firebase-functions/params");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
const { onSchedule } = require("firebase-functions/v2/scheduler")

// Initialize Firebase Admin SDK
admin.initializeApp();
const db = admin.firestore(); // --- NEW: Define db globally ---

// Define environment variables for email credentials.
// You'll set these using the CLI or in the Google Cloud console.
const gmailEmail = defineString("GMAIL_EMAIL");
const gmailPassword = defineString("GMAIL_PASSWORD");

// --- SETUP FOR SENDING EMAILS ---
const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {
        user: gmailEmail.value(),
        pass: gmailPassword.value(),
    },
});

// --- Callable Function to Mark ORDER as Paid ---
/**
 * Marks an order as completed/paid, adds a payment timestamp,
 * creates an income record, and increments the total income tracker.
 */
exports.markOrderAsPaid = onCall({ cors: true }, async (request) => {
    console.log("markOrderAsPaid called with data:", JSON.stringify(request.data));
    // 1. Authentication Check
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "You must be logged in.");
    }
    const callingUid = request.auth.uid;

    const { orderId } = request.data;
    if (!orderId) {
        throw new HttpsError("invalid-argument", "Missing 'orderId'.");
    }

    console.log(`Attempting to mark order ${orderId} as paid by user ${callingUid}.`);

    const orderRef = db.collection("product_orders").doc(orderId);
    const incomeRecordsRef = db.collection("income_records");
    const totalIncomeRef = db.collection("app_content").doc("income_tracking");

    let price = 0;
    let orderDataForIncomeRecord = null;

    try {
        // --- Transaction: Update Order Status ---
        await db.runTransaction(async (transaction) => {
            const orderDoc = await transaction.get(orderRef);
            if (!orderDoc.exists) { // Admin SDK uses .exists (property) - CORRECT
                throw new HttpsError("not-found", `Order ${orderId} not found.`);
            }
            const orderData = orderDoc.data();
            orderDataForIncomeRecord = orderData; // Store for later

            // 2. Authorization Check (Allow Admin or Worker)
            const userDocRef = db.collection("users").doc(callingUid);
            const userDoc = await transaction.get(userDocRef);

            // --- Access .exists as a property ---
            const userRole = userDoc.exists ? userDoc.data().role : null; // CORRECT

            if (userRole !== "ADMIN" && userRole !== "WORKER") {
                console.error(`Authorization failed: User ${callingUid} (Role: ${userRole}) is not an ADMIN or WORKER.`);
                throw new HttpsError("permission-denied", "Only Admins or Workers can mark orders as paid.");
            }

            // 3. Prevent re-processing
            if (orderData.status === "Completed" && orderData.paymentTimestamp) {
                throw new HttpsError("aborted", `Order ${orderId} was already marked as paid.`);
            }

            // 4. Get the price
            price = orderData.totalPrice || 0;
            console.log(`Fetched price: R${price}`);

            // 5. Update Order Status & Timestamp
            transaction.update(orderRef, {
                status: "Completed",
                paymentTimestamp: admin.firestore.FieldValue.serverTimestamp()
            });
            console.log(`Order ${orderId} status updated to Completed.`);
        }); // --- End of Transaction ---

        console.log(`Transaction successful for order ${orderId}. Price: R${price}`);

        // --- Post-Transaction ---
        // 6. Create Income Record
        const incomeRecordData = {
            amount: price,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            orderId: orderId,
            serviceName: `Product Order #${orderId.slice(-6)}`, // Use a descriptive name
            stylistId: null, // No specific stylist for a product order
            stylistName: "Salon Sale",
            customerId: orderDataForIncomeRecord?.customerId || null,
            customerName: orderDataForIncomeRecord?.customerName || 'N/A',
            type: 'order' // Indicate the source
        };
        await incomeRecordsRef.add(incomeRecordData);
        console.log(`Income record created for order ${orderId}.`);

        // 7. Update Total Income (Atomically)
        const totalIncomeDoc = await totalIncomeRef.get();
        if (!totalIncomeDoc.exists) {
            console.log(`Total income document not found. Creating with initial value: R${price}`);
            await totalIncomeRef.set({ totalIncome: price });
        } else {
            console.log(`Incrementing total income by R${price}.`);
            await totalIncomeRef.update({
                totalIncome: admin.firestore.FieldValue.increment(price)
            });
        }

        console.log(`Successfully processed payment for order ${orderId}.`);
        return { message: `Order ${orderId} marked as paid successfully.` };

    } catch (error) {
        if (error.code === 'aborted' && error.message.includes('already marked as paid')) {
            console.log(error.message);
            return { message: error.message };
        }
        console.error(`Error marking order ${orderId} as paid:`, error);
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", `An unexpected error occurred: ${error.message}`);
    }
});

// --- Callable Function to Mark Booking as Paid ---
/**
 * Marks a booking as completed/paid, adds a payment timestamp,
 * creates an income record, and increments the total income tracker.
 */
exports.markBookingAsPaid = onCall({ cors: true }, async (request) => {
    console.log("markBookingAsPaid called with data:", JSON.stringify(request.data));
    // 1. Authentication Check
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "You must be logged in.");
    }

    const { bookingId } = request.data;
    const callingUid = request.auth.uid;

    if (!bookingId) {
        throw new HttpsError("invalid-argument", "Missing 'bookingId'.");
    }

    console.log(`Attempting to mark booking ${bookingId} as paid by user ${callingUid}.`);

    const bookingRef = db.collection("bookings").doc(bookingId);
    const incomeRecordsRef = db.collection("income_records");
    const totalIncomeRef = db.collection("app_content").doc("income_tracking");

    let price = 0; // Variable to store price outside transaction scope
    let bookingDataForIncomeRecord = null; // Variable to store booking data for income record

    try {
        // --- Transaction: Update Booking Status ---
        await db.runTransaction(async (transaction) => {
            const bookingDoc = await transaction.get(bookingRef);
            if (!bookingDoc.exists) {
                throw new HttpsError("not-found", `Booking ${bookingId} not found.`);
            }
            const bookingData = bookingDoc.data();
            bookingDataForIncomeRecord = bookingData; // Store data for later use

            // 2. Authorization Check
            const userDocRef = db.collection("users").doc(callingUid);
            const userDocSnap = await transaction.get(userDocRef); // Fetch caller's user document INSIDE transaction

            // --- FIX: Use .exists (property) instead of .exists() (function) ---
            const userRole = userDocSnap.exists ? userDocSnap.data().role : null;
            const isAssignedStylist = bookingData.stylistId === callingUid;

            console.log(`Auth Check: Caller UID: ${callingUid}, Role: ${userRole}, Assigned Stylist ID: ${bookingData.stylistId}, Is Assigned: ${isAssignedStylist}`);

            // Check if user is an Admin or a Worker
            if (userRole !== "ADMIN" && userRole !== "WORKER") {
                console.error(`Authorization failed: User ${callingUid} (Role: ${userRole}) is not an ADMIN or WORKER.`);
                throw new HttpsError("permission-denied", "Only Admins or Workers can mark bookings as paid.");
            }
            // Optional: Stricter check to only allow the *assigned* worker or an admin
            // if (userRole !== "ADMIN" && !isAssignedStylist) {
            //     console.error(`Authorization failed: User ${callingUid} (Role: ${userRole}) is not an admin or the assigned stylist (${bookingData.stylistId}).`);
            //     throw new HttpsError("permission-denied", "You are not authorized to modify this booking.");
            // }

            // 3. Prevent re-processing
            if (bookingData.status === "Completed" && bookingData.paymentTimestamp) {
                console.log(`Booking ${bookingId} already marked paid. Skipping transaction.`);
                throw new HttpsError("aborted", `Booking ${bookingId} was already marked as paid.`); // Use aborted to stop transaction gracefully
            }

            // 4. Fetch the service price
            const hairstyleRef = db.collection("hairstyles").doc(bookingData.hairstyleId);
            const hairstyleDoc = await transaction.get(hairstyleRef); // GET INSIDE TRANSACTION
            if (!hairstyleDoc.exists) {
                throw new HttpsError("not-found", `Hairstyle (${bookingData.hairstyleId}) not found for booking ${bookingId}.`);
            }
            price = hairstyleDoc.data().price || 0; // Assign price to outer scope variable
            console.log(`Fetched price: R${price}`);

            // 5. Update Booking Status & Timestamp
            transaction.update(bookingRef, {
                status: "Completed",
                paymentTimestamp: admin.firestore.FieldValue.serverTimestamp()
            });
            console.log(`Booking ${bookingId} status updated to Completed.`);

        }); // --- End of Transaction ---

        console.log(`Transaction successful for booking ${bookingId}. Price: R${price}`);

        // --- Post-Transaction: Create Income Record & Update Total ---
        // These happen only if the transaction succeeded.

        // 6. Create Income Record
        const incomeRecordData = {
            amount: price,
            createdAt: admin.firestore.FieldValue.serverTimestamp(), // Use server timestamp
            bookingId: bookingId,
            serviceName: bookingDataForIncomeRecord?.serviceName || 'N/A',
            stylistId: bookingDataForIncomeRecord?.stylistId || null,
            stylistName: bookingDataForIncomeRecord?.stylistName || 'N/A',
            customerId: bookingDataForIncomeRecord?.customerId || null,
            customerName: bookingDataForIncomeRecord?.customerName || 'N/A',
            type: 'booking' // Indicate the source of income
        };
        await incomeRecordsRef.add(incomeRecordData);
        console.log(`Income record created for booking ${bookingId}.`);

        // 7. Update Total Income (Atomically)
        const totalIncomeDoc = await totalIncomeRef.get();
        if (!totalIncomeDoc.exists) {
            console.log(`Total income document not found. Creating with initial value: R${price}`);
            await totalIncomeRef.set({ totalIncome: price });
        } else {
            console.log(`Incrementing total income by R${price}.`);
            await totalIncomeRef.update({
                totalIncome: admin.firestore.FieldValue.increment(price)
            });
        }

        console.log(`Successfully processed payment for booking ${bookingId}.`);
        return { message: `Booking ${bookingId} marked as paid successfully.` };

    } catch (error) {
        // Handle the specific 'aborted' case for already paid bookings
        if (error.code === 'aborted' && error.message.includes('already marked as paid')) {
            console.log(error.message); // Log the specific skip message
            return { message: error.message }; // Return the skip message as success
        }

        // Log and rethrow other errors
        console.error(`Error marking booking ${bookingId} as paid:`, error);
        if (error instanceof HttpsError) {
            throw error;
        } else {
            throw new HttpsError("internal", `An unexpected error occurred: ${error.message}`);
        }
    }
});

/**
 * Sends a notification when a booking's status is updated.
 */
exports.onBookingStatusChange = onDocumentUpdated("bookings/{bookingId}", async (event) => {
    const newData = event.data.after.data();
    const oldData = event.data.before.data();
    const customerId = newData.customerId;

    // Only trigger if the status has actually changed
    if (newData.status === oldData.status) {
        console.log("Status unchanged, no notification sent.");
        return;
    }

    const bookingId = event.params.bookingId;
    if (!customerId) {
        console.error("Booking is missing a customerId!");
        return;
    }

    // Add decline reason to the message if applicable
    let messageBody = `Your appointment for ${newData.serviceName} has been ${newData.status.toLowerCase()}.`;
    if (newData.status === 'Declined' && newData.declineReason) {
        messageBody += ` Reason: ${newData.declineReason}`;
    }

    // Create the in-app notification in the user's subcollection
    const notificationPayload = {
        userId: customerId,
        title: `Booking ${newData.status}`,
        message: messageBody, // Use updated message body
        timestamp: Date.now(),
        isRead: false,
        bookingId: bookingId,
    };
    await admin.firestore().collection("users").doc(customerId).collection("notifications").add(notificationPayload);

    const userDoc = await admin.firestore().collection("users").doc(customerId).get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (fcmToken) {
        const message = {
                    token: fcmToken,
                    notification: {
                        title: `Booking ${newData.status}`,
                        body: messageBody,
                    },
                    data: {
                        title: `Booking ${newData.status}`,
                        body: messageBody,
                        bookingId: bookingId
                    },
                    android: {
                        priority: "high",
                        notification: {
                            sound: "default"
                        }
                    },
                    apns: {
                        payload: {
                            aps: {
                                sound: "default"
                            }
                        }
                    }
                };

                try {
                     console.log(`Attempting to send status change push notification to token: ${fcmToken}`);
                     await admin.messaging().send(message); // Use .send()
                     console.log(`Successfully sent status change push notification to user ${customerId}.`);
                } catch(error){
                     console.error(`Failed to send status change push notification to user ${customerId}:`, error);
                }
    }
});

/**
 * Sends a notification when a new chat message is sent.
 */
exports.onNewChatMessage = onDocumentCreated("bookings/{bookingId}/messages/{messageId}", async (event) => {
    const messageData = event.data.data();
    const bookingDoc = await admin.firestore().collection("bookings").doc(message.bookingId).get();
    const booking = bookingDoc.data();

    if (!booking) return;

    const senderUid = message.senderUid;
    const customerUid = booking.customerId;

    let recipientUid;
    if (senderUid === customerUid) {
        const stylistDoc = await admin.firestore().collection("users").where("name", "==", booking.stylistName).limit(1).get();
        recipientUid = stylistDoc.docs[0]?.id;
    } else {
        recipientUid = customerUid;
    }

    if (!recipientUid) {
        console.log("Could not determine recipient.");
        return;
    }

    const notificationPayload = {
        userId: recipientUid,
        title: `New Message from ${message.senderName}`,
        message: message.messageText,
        timestamp: Date.now(),
        isRead: false,
        // --- ADD bookingId for linking ---
        bookingId: message.bookingId
    };
    await admin.firestore().collection("users").doc(recipientUid).collection("notifications").add(notificationPayload);

    const recipientDoc = await admin.firestore().collection("users").doc(recipientUid).get();
    const fcmToken = recipientDoc.data()?.fcmToken;

    if (fcmToken) {
         const message = {
                     token: fcmToken,
                     notification: {
                          title: `New Message from ${messageData.senderName}`,
                          body: messageData.messageText,
                     },
                     data: {
                         title: `New Message from ${messageData.senderName}`,
                         body: messageData.messageText,
                         bookingId: messageData.bookingId
                     },
                     android: {
                         priority: "high",
                         notification: {
                             sound: "default"
                         }
                     },
                     apns: {
                         payload: {
                             aps: {
                                 sound: "default"
                             }
                         }
                     }
                  };
                  try {
                      console.log(`Attempting to send chat push notification to token: ${fcmToken}`);
                      await admin.messaging().send(message); // Use .send()
                      console.log(`Successfully sent chat push notification to user ${recipientUid}.`);
                  } catch(error){
                      console.error(`Failed to send chat push notification to user ${recipientUid}:`, error);
                  }
    }
});

/**
 * v2 Cloud Function to send a push notification to all admins
 * when a new support message is created.
 */
exports.sendSupportPushNotification = onDocumentCreated("support_messages/{messageId}", async(event) => {
    // ... function unchanged ...
    const snap = event.data;
    if (!snap) { console.log("No data associated with the event"); return; }
     const messageData = snap.data();

    const usersRef = admin.firestore().collection("users");
    const adminQuery = await usersRef.where("role", "==", "ADMIN").get();

    const tokens = [];
    adminQuery.forEach((doc) => {
        const token = doc.data().fcmToken;
        if (token) { tokens.push(token); }
    });

    if (tokens.length > 0) {
         const message = {
                     tokens: tokens, // Use 'tokens' (plural)
                     notification: {
                          title: `New Support Message from ${messageData.senderName}`,
                          body: messageData.message,
                     },
                     data: {
                         title: `New Support Message from ${messageData.senderName}`,
                         body: messageData.message,
                         ticketId: snap.id
                     },
                     android: {
                         priority: "high",
                         notification: {
                             sound: "default"
                         }
                     },
                     apns: {
                         payload: {
                             aps: {
                                 sound: "default"
                             }
                         }
                     }
                  };
                  try {
                      console.log(`Attempting to send support push notification to ${tokens.length} admin tokens.`);
                      await admin.messaging().sendEachForMulticast(message); // Use .sendEachForMulticast()
                      console.log(`Successfully sent support push notification.`);
                  } catch(error){
                      console.error(`Failed to send support push notification:`, error);
                  }
    }
});

/**
 * v2 Cloud Function to send an email notification when a new support
 * message is created.
 */
exports.sendSupportEmail = onDocumentCreated("support_messages/{messageId}", async(event) => {
    const snap = event.data;
    if (!snap) {
        console.log("No data associated with the event");
        return;
    }
    const data = snap.data();


    const transporter = nodemailer.createTransport({
        service: "gmail",
        auth: {
            user: gmailEmail.value(),
            pass: gmailPassword.value(),
        },
    });

    const mailOptions = {
        from: `GDMS <${gmailEmail.value()}>`,
        to: "whiteshytk@gmail.com", // Your support email
        subject: `New Support Message from ${data.senderName}`,
        html: `<h1>New Support Ticket</h1><p><b>From:</b> ${data.senderName} (${data.senderEmail})</p><p><b>Message:</b></p><p>${data.message}</p>`,
    };

    try {
        await transporter.sendMail(mailOptions);
        console.log("Support email sent successfully.");
    } catch (error) {
        console.error("Error sending support email:", error);
    }
});

/**
 * v2 Callable function that allows an admin to set a custom role.
 * UPDATED with { cors: true }
 */
exports.setUserRole = onCall({ cors: true }, async (request) => {
    if (!request.auth || !request.auth.token || request.auth.token.admin !== true) {
        throw new HttpsError("permission-denied", "Request not authorized. User must be an admin to set roles.");
    }
    const { userId, role } = request.data;
    try {
        const lowerCaseRole = role.toLowerCase();
        await admin.auth().setCustomUserClaims(userId, { [lowerCaseRole]: true, admin: role === 'ADMIN' });
        await admin.firestore().collection("users").doc(userId).update({ role: role.toUpperCase() });
        return { message: `Success! User ${userId} has been made a ${role.toUpperCase()}.` };
    } catch (error) {
        console.error("Error setting user role:", error);
        throw new HttpsError("internal", error.message);
    }
});


/**
 * v2 Callable function that allows an admin to delete a user.
 * UPDATED with { cors: true }
 */
exports.deleteUser = onCall({ cors: true }, async (request) => {
    if (!request.auth || !request.auth.token || request.auth.token.admin !== true) {
        throw new HttpsError("permission-denied", "Request not authorized. User must be an admin.");
    }
    const { userId } = request.data;
    try {
        await admin.auth().deleteUser(userId);
        await admin.firestore().collection("users").doc(userId).delete();
        return { message: `Successfully deleted user ${userId}.` };
    } catch (error) {
        console.error("Error deleting user:", error);
        throw new HttpsError("internal", error.message);
    }
});

/**
 * A scheduled function that runs automatically every 24 hours
 * to clean up old, missed appointments.
 */
exports.autoCancelMissedBookings = onSchedule("every 24 hours", async (event) => {
    console.log("Running daily check for missed bookings...");

    const now = admin.firestore.Timestamp.now();
    const db = admin.firestore();

    // Find all bookings with a timestamp in the past
    // that are still marked as 'Pending' or 'Confirmed'.
    const query = db.collection("bookings")
        .where("timestamp", "<", now)
        .where("status", "in", ["Pending", "Confirmed"]);

    const missedBookings = await query.get();

    if (missedBookings.empty) {
        console.log("No missed bookings found.");
        return;
    }

    // Create a batch write to update all missed bookings at once
    const batch = db.batch();
    missedBookings.forEach(doc => {
        console.log(`Marking booking ${doc.id} as Missed.`);
        batch.update(doc.ref, { status: "Missed" });
    });

    // Commit the batch
    await batch.commit();
    console.log(`Successfully updated ${missedBookings.size} bookings.`);
    return;
});

/**
 * v2 Callable function that securely calculates available slots.
 */
exports.getAvailableSlots = onCall({ cors: true }, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "You must be logged in to check availability.");
    }

  const { stylistName, date, hairstyleId } = request.data;
  const db = admin.firestore();

  try {
    // FIX: Removed all date normalization. The function now trusts the client (both app and web)
    // to send the date in the standardized "YYYY-MM-DD" format.
    console.log(`Request for date: "${date}" (Expecting YYYY-MM-DD)`);


    const hoursDoc = await db.collection("app_content").doc("salon_hours").get();
    if (!hoursDoc.exists) throw new HttpsError("not-found", "Salon hours are not configured.");
    const allSlots = hoursDoc.data().time_slots || [];

    const hairstyleDoc = await db.collection("hairstyles").doc(hairstyleId).get();
    if (!hairstyleDoc.exists) throw new HttpsError("not-found", "The selected hairstyle does not exist.");

    const durationInSlots = Math.ceil(hairstyleDoc.data().durationHours) || 1;

      // --- 2. Check for Stylist Sick Days / Time Off (NEW) ---
        // First, find the stylist's ID from their name
        const stylistQuery = db.collection("users")
            .where("name", "==", stylistName)
            .where("role", "==", "WORKER")
            .limit(1);
        const stylistSnapshot = await stylistQuery.get();

        if (stylistSnapshot.empty) {
            console.error(`No stylist found with name: ${stylistName}`);
            throw new HttpsError("not-found", `Stylist ${stylistName} not found.`);
        }
        
        const stylistId = stylistSnapshot.docs[0].id;

        // Now, check if this stylist has approved time off for the requested date
        const timeOffQuery = db.collection("timeOffRequests")
            .where("stylistId", "==", stylistId)
            .where("status", "==", "approved")
            .where("startDate", "<=", date); // Find requests that started on or before this date
        
        const timeOffSnapshot = await timeOffQuery.get();

        let isOffToday = false;
        if (!timeOffSnapshot.empty) {
            for (const doc of timeOffSnapshot.docs) {
                const timeOff = doc.data();
                // Check if the end date is on or after the requested date
                if (timeOff.endDate >= date) {
                    isOffToday = true;
                    break; // Found a valid time-off, no need to check others
                }
            }
        }

        if (isOffToday) {
            console.log(`Stylist ${stylistName} (ID: ${stylistId}) is on approved time off for ${date}.`);
            return { slots: [] }; // Return empty slots immediately
        }
        // --- END OF SICK DAY LOGIC ---

    // FIX: The query now uses the 'date' string directly, expecting it to be "YYYY-MM-DD".
    const bookingsQuery = db.collection("bookings")
        .where("stylistName", "==", stylistName)
        .where("date", "==", date)
        .where("status", "in", ["Confirmed", "Pending"]);

    const bookingsSnapshot = await bookingsQuery.get();
    const occupiedSlots = new Set();

    for (const bookingDoc of bookingsSnapshot.docs) {
        const booking = bookingDoc.data();
        const bookedHairstyleDoc = await db.collection("hairstyles").doc(booking.hairstyleId).get();
        if (!bookedHairstyleDoc.exists) continue;

        const bookedDuration = Math.ceil(bookedHairstyleDoc.data().durationHours) || 1;
        const startTime = booking.time;
        const startIndex = allSlots.indexOf(startTime);

        if (startIndex !== -1) {
            for (let i = 0; i < bookedDuration; i++) {
                if (startIndex + i < allSlots.length) {
                    occupiedSlots.add(allSlots[startIndex + i]);
                }
            }
        }
    }

    const availableSlots = allSlots.filter(slot => !occupiedSlots.has(slot));

    const finalSlots = [];
    for (let i = 0; i <= availableSlots.length - durationInSlots; i++) {
        let hasEnoughTime = true;
        for (let j = 1; j < durationInSlots; j++) {
            const expectedNextSlotIndex = allSlots.indexOf(availableSlots[i]) + j;
            const actualNextSlotIndex = allSlots.indexOf(availableSlots[i+j]);
            if (expectedNextSlotIndex !== actualNextSlotIndex) {
                hasEnoughTime = false;
                break;
            }
        }
        if (hasEnoughTime) {
            finalSlots.push(availableSlots[i]);
        }
    }

    return { slots: finalSlots };

  } catch (error) {
    console.error("Error getting available slots:", error);
    throw new HttpsError("internal", "Failed to get available slots.");
  }
});

/**
 * A scheduled function that runs automatically every hour to send
 * reminders for appointments happening in the next hour.
 */
exports.sendBookingReminders = onSchedule("every 1 hours", async (event) => {
    console.log("Running hourly check for upcoming booking reminders...");

    const now = new Date();
    const oneHourFromNow = new Date(now.getTime() + 60 * 60 * 1000);
    const db = admin.firestore();

    // Find all confirmed bookings with a timestamp in the next hour.
    const query = db.collection("bookings")
        .where("bookingTimestamp", ">=", now)
        .where("bookingTimestamp", "<=", oneHourFromNow)
        .where("status", "==", "Confirmed");

    const upcomingBookings = await query.get();

    if (upcomingBookings.empty) {
        console.log("No upcoming bookings to send reminders for.");
        return;
    }

    const notificationPromises = [];
    upcomingBookings.forEach(doc => {
        const booking = doc.data();
        const bookingId = doc.id;

        // --- Create Notification for the Customer ---
        const customerNotification = {
            userId: booking.customerId,
            title: "Appointment Reminder",
            message: `Your appointment for ${booking.serviceName} with ${booking.stylistName} is in about an hour.`,
            timestamp: Date.now(),
            isRead: false,
            bookingId: bookingId, 
        };
        const customerPromise = db.collection("users").doc(booking.customerId).collection("notifications").add(customerNotification);
        notificationPromises.push(customerPromise);

        // --- Create Notification for the Worker ---
        const workerId = booking.stylistId;
        if (workerId) {
             const workerNotification = {
                userId: workerId,
                title: "Appointment Reminder",
                message: `Your appointment with ${booking.customerName} for ${booking.serviceName} is in about an hour.`,
                timestamp: Date.now(),
                isRead: false,
                bookingId: bookingId,
            };
            const workerPromise = db.collection("users").doc(workerId).collection("notifications").add(workerNotification);
            notificationPromises.push(workerPromise);
        }
        console.log(`Queued reminder for booking ${doc.id}`);
    });

    // Wait for all the notification writes to complete in parallel
    await Promise.all(notificationPromises);
    console.log(`Successfully sent reminders for ${upcomingBookings.size} bookings.`);

    return;
});


/**
 * A scheduled function that runs automatically every hour
 * to mark past-due confirmed bookings as 'Completed'.
 */
exports.autoCompleteBookings = onSchedule("every 1 hours", async (event) => {
    console.log("Running hourly check for completed bookings...");

    const now = admin.firestore.Timestamp.now();
    const db = admin.firestore();

    // Find all bookings with a timestamp in the past
    // that are still marked as 'Confirmed'.
    const query = db.collection("bookings")
        .where("bookingTimestamp", "<", now)
        .where("status", "==", "Confirmed");

    const pastBookings = await query.get();

    if (pastBookings.empty) {
        console.log("No past-due bookings found to complete.");
        return;
    }

    // Create a batch write to update all of them at once for efficiency
    const batch = db.batch();
    pastBookings.forEach(doc => {
        console.log(`Marking booking ${doc.id} as Completed.`);
        batch.update(doc.ref, { status: "Completed" });
    });

    // Commit all the changes
    await batch.commit();
    console.log(`Successfully completed ${pastBookings.size} bookings.`);

    return;
});

/**
 * v2 Cloud Function that sends a notification when a new reply is added
 * to a support ticket.
 */
exports.onNewSupportReply = onDocumentCreated("support_messages/{ticketId}/replies/{replyId}", async (event) => {
    const reply = event.data.data();
    const ticketId = event.params.ticketId;
    const db = admin.firestore();

    try {
        const ticketDoc = await db.collection("support_messages").doc(ticketId).get();
        const ticket = ticketDoc.data();
        if (!ticket) {
            console.log("Original ticket not found.");
            return;
        }

        const senderUid = reply.senderUid;
        const customerUid = ticket.senderUid;

        let recipientUid;
        if (senderUid === customerUid) {
            // If the customer sent the reply, notify all admins.
            // TODO: Implement notifying specific admins or using a topic
            console.log("Customer replied to a ticket. Admin notifications for replies are not yet fully implemented.");
             // Send push to admins anyway
             const usersRef = admin.firestore().collection("users");
             const adminQuery = await usersRef.where("role", "==", "ADMIN").get();
             const tokens = [];
             adminQuery.forEach((doc) => {
                 const token = doc.data().fcmToken;
                 if (token) { tokens.push(token); }
             });
             if (tokens.length > 0) {
                const message = {
                                    tokens: tokens,
                                     notification: {
                                         title: `Reply on Ticket #${ticketId.slice(-6)} from ${reply.senderName}`,
                                         body: reply.messageText
                                     },
                                     data: {
                                        title: `Reply on Ticket #${ticketId.slice(-6)} from ${reply.senderName}`,
                                        body: reply.messageText,
                                        ticketId: ticketId
                                    },
                                    android: { priority: "high", notification: { sound: "default" } },
                                    apns: { payload: { aps: { sound: "default" } } }
                                 };
                                 await admin.messaging().sendEachForMulticast(message); // Use .sendEachForMulticast()
                                 console.log(`Push notification sent to admins for ticket reply.`);
                                 // --- END FIX ---
                             }
                             return;
        } else {
            // If the admin sent the reply, notify the customer.
            recipientUid = customerUid;
        }

        // Create the in-app notification for the customer.
        const notificationPayload = {
            userId: recipientUid,
            title: `New Reply from ${reply.senderName}`,
            message: reply.messageText,
            timestamp: Date.now(),
            isRead: false,
            ticketId: ticketId,
        };
        await db.collection("users").doc(recipientUid).collection("notifications").add(notificationPayload);

        // Send the push notification to the customer.
        const recipientDoc = await db.collection("users").doc(recipientUid).get();
        const fcmToken = recipientDoc.data()?.fcmToken;

        if (fcmToken) {
           const message = {
                           token: fcmToken,
                           notification: {
                               title: notificationPayload.title,
                               body: notificationPayload.message,
                           },
                            data: {
                               title: notificationPayload.title,
                               body: notificationPayload.message,
                               ticketId: ticketId
                            },
                           android: { priority: "high", notification: { sound: "default" } },
                           apns: { payload: { aps: { sound: "default" } } }
                       };
                       await admin.messaging().send(message); // Use .send()
                       console.log(`Push notification sent for ticket reply to user: ${recipientUid}`);
                       // --- END FIX ---
                   }
    } catch (error) {
        console.error("Error in onNewSupportReply function:", error);
    }
});

/**
 * Triggers when a new chat message is created. If the sender is a customer,
 * it increments an unread counter on the parent booking for the worker.
 */
exports.onNewChatMessageForWorker = onDocumentCreated("bookings/{bookingId}/messages/{messageId}", async (event) => {
    const message = event.data.data();
    const bookingRef = admin.firestore().collection("bookings").doc(event.params.bookingId);

    const bookingDoc = await bookingRef.get();
    const booking = bookingDoc.data();

    // Only proceed if the sender is the customer
    if (booking && message.senderUid === booking.customerId) {
        console.log(`Customer sent new message for booking ${event.params.bookingId}. Incrementing worker unread count.`);
        // Atomically increment the counter on the booking document
        return bookingRef.update({
            workerUnreadCount: admin.firestore.FieldValue.increment(1)
        });
    }
    return null;
});

/**
 * Triggers when a product order's status is updated.
 */
exports.onOrderReadyForPickup = onDocumentUpdated("product_orders/{orderId}", async (event) => {
    const newData = event.data.after.data();
    const oldData = event.data.before.data();
    const db = admin.firestore();
    const orderId = event.params.orderId;

    // --- Customer Notification Logic ---
    const customerId = newData.customerId;
    if (!customerId) {
        console.error(`Order ${orderId} is missing a customerId!`);
    } else {
        let notificationTitle = "";
        let notificationMessage = "";
        let shouldNotify = false;

         // Determine notification based on status change
        if (newData.status === "Ready for Pickup" && oldData.status !== "Ready for Pickup") {
            notificationTitle = "Your Order is Ready!";
            notificationMessage = `Your order #${orderId.slice(-6)} is now ready for pickup at the salon.`;
            shouldNotify = true;
        } else if (newData.status === "Completed" && oldData.status !== "Completed") {
            // Optional: Notify on completion?
             notificationTitle = "Order Completed";
             notificationMessage = `Your order #${orderId.slice(-6)} has been marked as completed. Thank you!`;
             shouldNotify = true;
        } else if (newData.status === "Abandoned" && oldData.status !== "Abandoned") {
             notificationTitle = "Order Abandoned";
             notificationMessage = `Your order #${orderId.slice(-6)} was not picked up and has been marked as abandoned. Please contact us if this was a mistake.`;
             shouldNotify = true;
        }
         // Add more cases if needed (e.g., Shipped, Delivered)

        if (shouldNotify) {
            const notificationPayload = {
                userId: customerId,
                title: notificationTitle,
                message: notificationMessage,
                timestamp: Date.now(),
                isRead: false,
                orderId: orderId, // Link notification to the order
            };
            // 1. Create the in-app notification
            await db.collection("users").doc(customerId).collection("notifications").add(notificationPayload);

            // 2. Send the push notification
            try {
                const userDoc = await db.collection("users").doc(customerId).get();
                const fcmToken = userDoc.data()?.fcmToken;

                if (fcmToken) {
                    const message = {
                                            token: fcmToken,
                                            notification: {
                                                title: notificationPayload.title,
                                                body: notificationPayload.message,
                                            },
                                           data: {
                                                title: notificationPayload.title,
                                                body: notificationPayload.message,
                                                orderId: orderId
                                           },
                                           android: { priority: "high", notification: { sound: "default" } },
                                           apns: { payload: { aps: { sound: "default" } } }
                                        };
                                        console.log(`Attempting to send order status (${newData.status}) push notification to token: ${fcmToken}`);
                                        await admin.messaging().send(message); // Use .send()
                                        console.log(`Successfully sent order status push notification to user ${customerId}.`);
                                        // --- END FIX ---
                } else {
                     console.log(`User ${customerId} does not have an FCM token. Skipping push notification.`);
                }
            } catch (error) {
                console.error(`Failed to send order status push notification to user ${customerId}:`, error);
                // Log specific error details if available
                if (error.errorInfo) {
                    console.error("FCM Error Info:", JSON.stringify(error.errorInfo));
                }
                if (error.codePrefix === 'messaging') {
                    console.error("FCM Error Code:", error.errorInfo.code);
                }
                 // Handle common errors like unregistered tokens if needed
                 if (error.errorInfo && (error.errorInfo.code === 'messaging/registration-token-not-registered' || error.errorInfo.code === 'messaging/invalid-registration-token')) {
                     console.log(`Token ${fcmToken} is invalid or unregistered. Consider removing it from user ${customerId}.`);
                     // Optionally, remove the token: await db.collection("users").doc(customerId).update({ fcmToken: admin.firestore.FieldValue.delete() });
                 }
            }
        }
    }
    // --- END OF NOTIFICATION BLOCK ---


    // --- Stock Update Logic (Only for 'Ready for Pickup') ---
    if (newData.status === "Ready for Pickup" && oldData.status !== "Ready for Pickup") {
        console.log(`Processing stock deduction for order ${orderId}`);
        const items = newData.items;
        if (!items || items.length === 0) {
             console.log("No items found in order to deduct stock.");
             return null; // Nothing more to do if no items
        }

        for (const item of items) {
            const productRef = db.collection("products").doc(item.productId);
            try {
                await db.runTransaction(async (transaction) => {
                    const productDoc = await transaction.get(productRef);
                    if (!productDoc.exists) { throw `Product ${item.productId} not found!`; }
                    const productData = productDoc.data();
                    const variants = productData.variants;
                    let stockUpdated = false;
                    const updatedVariants = variants.map(variant => {
                        if (variant.size === item.size) {
                            const currentStock = variant.stock || 0;
                            const newStock = currentStock - item.quantity;
                            stockUpdated = true;
                            if (newStock <= 5 && currentStock > 5) {
                                console.log(`LOW STOCK ALERT for ${productData.name} (${variant.size})`);
                                notifyAdminsOfLowStock(productData, variant, newStock);
                            }
                            // --- IMPORTANT FIX: Allow stock to go negative temporarily ---
                            // This handles cases where stock might be slightly off or orders processed concurrently.
                            // Rely on inventory management elsewhere to resolve negative stock.
                            console.log(`Deducting ${item.quantity} from ${productData.name} (${variant.size}). Old stock: ${currentStock}, New stock: ${newStock}`);
                            return { ...variant, stock: newStock };
                             // return { ...variant, stock: Math.max(0, newStock) }; // Old way - prevents negative
                        }
                        return variant;
                    });
                    if (stockUpdated) {
                        transaction.update(productRef, { variants: updatedVariants });
                    }
                });
                console.log(`Stock updated for ${item.productId}`);
            } catch (e) {
                console.error(`Stock update transaction failed for order ${orderId}, item ${item.productId}:`, e);
                 // CRITICAL: Decide how to handle this. Should the order status be reverted? Log for manual intervention?
                 // For now, just logging the error.
            }
        }
    } // End of stock deduction block

    return null; // Finish the function
});


// --- HELPER FUNCTION FOR LOW-STOCK NOTIFICATIONS ---
/**
 * Finds all admin users and sends them a low-stock alert.
 * @param {object} productData The data of the product that is low on stock.
 * @param {object} variant The specific product variant that is low.
 * @param {number} newStock The new stock level.
 */
async function notifyAdminsOfLowStock(productData, variant, newStock) {
    const db = admin.firestore();
    const usersRef = db.collection("users");
    const adminQuery = await usersRef.where("role", "==", "ADMIN").get();

    if (adminQuery.empty) {
        console.log("No admins found to notify.");
        return;
    }

    const notificationPayload = {
        title: "Low Stock Alert!",
        message: `${productData.name} (${variant.size}) is running low. Only ${newStock} left in stock.`,
        timestamp: Date.now(),
        isRead: false,
        // Optional: productId: productData.id, // If productData includes the ID
    };

    const tokens = [];
        for (const adminDoc of adminQuery.docs) {
            const adminUser = adminDoc.data();
            // 1. Create in-app notification
            await db.collection("users").doc(adminDoc.id).collection("notifications").add(notificationPayload);
            // 2. Collect tokens
            if (adminUser.fcmToken) {
                tokens.push(adminUser.fcmToken);
            }
        }

        // 3. Send push notification to all admins at once
        if (tokens.length > 0) {
            // --- FIX: Replaced sendToDevice with sendEachForMulticast() ---
            const message = {
                tokens: tokens,
                notification: {
                    title: notificationPayload.title,
                    body: notificationPayload.message,
                },
                data: {
                    title: notificationPayload.title,
                    body: notificationPayload.message,
                    // Optional: productId: productData.id
                },
                android: { priority: "high", notification: { sound: "default" } },
                apns: { payload: { aps: { sound: "default" } } }
            };
            try {
                await admin.messaging().sendEachForMulticast(message); // Use .sendEachForMulticast()
                console.log(`Sent low-stock alerts to ${tokens.length} admins.`);
            } catch (error) {
                 console.error(`Failed to send low stock push to admins:`, error);
            }
            // --- END FIX ---
        }
    }

/**
 * v2 Callable function that allows an admin to create a new user.
 * This is the new function that was missing.
 */
exports.createUserByAdmin = onCall({ cors: true }, async (request) => {
    if (!request.auth || !request.auth.token || request.auth.token.admin !== true) {
        throw new HttpsError("permission-denied", "Request not authorized. User must be an admin.");
    }
    const { email, password, name, phone, role } = request.data;
    try {
        const userRecord = await admin.auth().createUser({ email, password, displayName: name });
        const userId = userRecord.uid;
        const lowerCaseRole = role.toLowerCase();
        await admin.auth().setCustomUserClaims(userId, { [lowerCaseRole]: true, admin: role === 'ADMIN' });
        await admin.firestore().collection("users").doc(userId).set({
            id: userId, name, email, phone, role: role.toUpperCase(), imageUrl: "", createdAt: admin.firestore.FieldValue.serverTimestamp() // Add creation timestamp
        });
        return { message: `Successfully created user ${name} with role ${role}.`, userId: userId }; // Return userId
    } catch (error) {
        console.error("Error in createUserByAdmin:", error);
        throw new HttpsError("internal", error.message);
    }
});


/**
 * A scheduled function that runs automatically every 24 hours to find
 * old orders that were never picked up and return their stock.
 */
exports.autoAbandonOldOrders = onSchedule("every 24 hours", async (event) => {
    console.log("Running daily check for abandoned product orders...");
    const db = admin.firestore();

    // Calculate the timestamp for 7 days ago
    const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;

    // Query for all orders that are "Ready for Pickup" and are older than 7 days
    const oldOrdersQuery = db.collection("product_orders")
        .where("status", "==", "Ready for Pickup")
        .where("timestamp", "<=", sevenDaysAgo);

    const oldOrdersSnapshot = await oldOrdersQuery.get();

    if (oldOrdersSnapshot.empty) {
        console.log("No abandoned orders found.");
        return null;
    }

    console.log(`Found ${oldOrdersSnapshot.size} abandoned orders to process.`);

    // Use a batch write to update all the order statuses at once
    const batch = db.batch();

    // Loop through each abandoned order to process its stock
    for (const orderDoc of oldOrdersSnapshot.docs) {
        const order = orderDoc.data();

        // 1. Mark the order as 'Abandoned' in the batch
        batch.update(orderDoc.ref, { status: "Abandoned" });
        console.log(`Marking order ${orderDoc.id} as Abandoned.`);

        // 2. Return the stock for each item in the order using secure transactions
        for (const item of order.items) {
            const productRef = db.collection("products").doc(item.productId);
            try {
                await db.runTransaction(async (transaction) => {
                    const productDoc = await transaction.get(productRef);
                    if (!productDoc.exists) {
                        throw `Product ${item.productId} not found!`;
                    }

                    const productData = productDoc.data();
                    const variants = productData.variants;

                    const updatedVariants = variants.map(variant => {
                        if (variant.size === item.size) {
                            // Safely add the stock back
                            const newStock = (variant.stock || 0) + item.quantity;
                            console.log(`Returning ${item.quantity} units to ${productData.name} (${variant.size}). New stock: ${newStock}`);
                            return { ...variant, stock: newStock };
                        }
                        return variant;
                    });

                    transaction.update(productRef, { variants: updatedVariants });
                });
            } catch (e) {
                console.error(`Failed to return stock for item ${item.productId} in order ${orderDoc.id}:`, e);
                // We continue even if one item fails, to process the rest
            }
        }
    }

    // 3. Commit all the status updates for the orders
    await batch.commit();
    console.log("Finished processing abandoned orders.");
    return null;
});

/**
 * Triggers when a product order's status is updated. If the new status is
 * 'Cancelled', it securely returns the item quantities to the main product stock.
 */
exports.onOrderCancelled = onDocumentUpdated("product_orders/{orderId}", async (event) => {
    const newData = event.data.after.data();
    const oldData = event.data.before.data();
    const db = admin.firestore();

    // Only run if the status changed specifically TO 'Cancelled'
    if (newData.status !== "Cancelled" || oldData.status === "Cancelled") {
        return null;
    }

    console.log(`Processing stock return for cancelled order ${event.params.orderId}`);
    const items = newData.items;
    if (!items || items.length === 0) {
        console.log("No items found in the order to return.");
        return null;
    }

    // Loop through each item in the cancelled order to return its stock
    for (const item of items) {
        const productRef = db.collection("products").doc(item.productId);
        try {
            await db.runTransaction(async (transaction) => {
                const productDoc = await transaction.get(productRef);
                if (!productDoc.exists) {
                    throw `Product ${item.productId} not found!`;
                }

                const productData = productDoc.data();
                const variants = productData.variants;

                const updatedVariants = variants.map(variant => {
                    if (variant.size === item.size) {
                        const newStock = (variant.stock || 0) + item.quantity;
                        console.log(`Returning ${item.quantity} units to ${productData.name} (${variant.size}). New stock: ${newStock}`);
                        return { ...variant, stock: newStock };
                    }
                    return variant;
                });

                transaction.update(productRef, { variants: updatedVariants });
            });
        } catch (e) {
            console.error(`Failed to return stock for item ${item.productId} in cancelled order ${event.params.orderId}:`, e);
        }
    }
    return null;
});

/**
 * A scheduled function that runs automatically every hour to clean up
 * past-due bookings. It marks old 'Confirmed' bookings as 'Missed' and
 * old 'Pending' bookings as 'Expired'.
 */
exports.cleanupPastDueBookings = onSchedule("every 1 hours", async (event) => {
    console.log("Running hourly check for past-due bookings...");

    const now = admin.firestore.Timestamp.now();
    const db = admin.firestore();

    // Query for all bookings with a timestamp in the past
    // that are still marked as 'Confirmed' OR 'Pending'.
    const query = db.collection("bookings")
        .where("bookingTimestamp", "<", now)
        .where("status", "in", ["Confirmed", "Pending"]);

    const pastDueBookings = await query.get();

    if (pastDueBookings.empty) {
        console.log("No past-due bookings found to update.");
        return null;
    }

    // Use a batch write to update all bookings at once for efficiency
    const batch = db.batch();
    pastDueBookings.forEach(doc => {
        const booking = doc.data();
        // Use a different status depending on the original status
        const newStatus = booking.status === "Confirmed" ? "Missed" : "Expired";

        console.log(`Marking booking ${doc.id} (status: ${booking.status}) as ${newStatus}.`);
        batch.update(doc.ref, { status: newStatus });
    });

    // Commit all the changes
    await batch.commit();
    console.log(`Successfully cleaned up ${pastDueBookings.size} past-due bookings.`);

    return null;
});


