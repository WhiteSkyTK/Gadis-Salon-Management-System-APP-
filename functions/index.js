const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { defineString } = require("firebase-functions/params");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
const { onSchedule } = require("firebase-functions/v2/scheduler")

// Initialize Firebase Admin SDK
admin.initializeApp();

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

    // Create the in-app notification in the user's subcollection
    const notificationPayload = {
        userId: customerId,
        title: `Booking ${newData.status}`,
        message: `Your appointment for ${newData.serviceName} has been ${newData.status.toLowerCase()}.`,
        timestamp: Date.now(),
        isRead: false,
        bookingId: bookingId,
    };
    await admin.firestore().collection("users").doc(customerId).collection("notifications").add(notificationPayload);

    const userDoc = await admin.firestore().collection("users").doc(customerId).get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (fcmToken) {
        const pushPayload = {
            notification: {
                title: `Booking ${newData.status}`,
                body: `Your appointment for ${newData.serviceName} has been ${newData.status.toLowerCase()}.`,
                sound: "default" // <-- ADD THIS LINE
            },
        };
        await admin.messaging().sendToDevice(fcmToken, pushPayload);
    }
});

/**
 * Sends a notification when a new chat message is sent.
 */
exports.onNewChatMessage = onDocumentCreated("bookings/{bookingId}/messages/{messageId}", async (event) => {
    const message = event.data.data();
    const bookingDoc = await admin.firestore().collection("bookings").doc(message.bookingId).get();
    const booking = bookingDoc.data();

    if (!booking) return;

    const senderUid = message.senderUid;
    const customerUid = booking.customerId;

    // Determine who the recipient is
    let recipientUid;
    if (senderUid === customerUid) {
        // If the customer sent it, notify the stylist
        const stylistDoc = await admin.firestore().collection("users").where("name", "==", booking.stylistName).limit(1).get();
        recipientUid = stylistDoc.docs[0]?.id;
    } else {
        // If the stylist sent it, notify the customer
        recipientUid = customerUid;
    }

    if (!recipientUid) {
        console.log("Could not determine recipient.");
        return;
    }

    // Create the in-app notification
    const notificationPayload = {
        userId: recipientUid,
        title: `New Message from ${message.senderName}`,
        message: message.messageText,
        timestamp: Date.now(),
        isRead: false,
    };
    await admin.firestore().collection("users").doc(recipientUid).collection("notifications").add(notificationPayload);

    // Send the push notification
    const recipientDoc = await admin.firestore().collection("users").doc(recipientUid).get();
    const fcmToken = recipientDoc.data()?.fcmToken;

    if (fcmToken) {
            const pushPayload = {
                notification: {
                    title: `New Message from ${message.senderName}`,
                    body: message.messageText,
                    sound: "default" // <-- ADD THIS LINE
                }
            };
            await admin.messaging().sendToDevice(fcmToken, pushPayload);
        }
    });

/**
 * v2 Cloud Function to send a push notification to all admins
 * when a new support message is created.
 */
exports.sendSupportPushNotification = onDocumentCreated("support_messages/{messageId}", async(event) => {
    const snap = event.data;
    if (!snap) {
        console.log("No data associated with the event");
        return;
    }
    const message = snap.data();
    const payload = {
        notification: {
            title: `New Support Message from ${message.senderName}`,
            body: message.message,
        },
    };

    const usersRef = admin.firestore().collection("users");
    const adminQuery = await usersRef.where("role", "==", "ADMIN").get();

    const tokens = [];
    adminQuery.forEach((doc) => {
        const token = doc.data().fcmToken;
        if (token) {
            tokens.push(token);
        }
    });

    if (tokens.length > 0) {
            const payload = {
                notification: {
                    title: `New Support Message from ${message.senderName}`,
                    body: message.message,
                    sound: "default" // <-- ADD THIS LINE
                },
            };
            return admin.messaging().sendToDevice(tokens, payload);
        }
        return null;
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
        .where("timestamp", ">=", now)
        .where("timestamp", "<=", oneHourFromNow)
        .where("status", "==", "Confirmed");

    const upcomingBookings = await query.get();

    if (upcomingBookings.empty) {
        console.log("No upcoming bookings to send reminders for.");
        return;
    }

    const notificationPromises = [];
    upcomingBookings.forEach(doc => {
        const booking = doc.data();

        // --- Create Notification for the Customer ---
        const customerNotification = {
            userId: booking.customerId,
            title: "Appointment Reminder",
            message: `Your appointment for ${booking.serviceName} with ${booking.stylistName} is in about an hour.`,
            timestamp: Date.now(),
            isRead: false,
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
        .where("timestamp", "<", now)
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
 * to a support ticket. new
 */
exports.onNewSupportReply = onDocumentCreated("support_messages/{ticketId}/replies/{replyId}", async (event) => {
    const reply = event.data.data();
    const ticketId = event.params.ticketId;
    const db = admin.firestore();

    try {
        // Step 1: Get the original support ticket to find out who the customer is.
        const ticketDoc = await db.collection("support_messages").doc(ticketId).get();
        const ticket = ticketDoc.data();
        if (!ticket) {
            console.log("Original ticket not found.");
            return;
        }

        const senderUid = reply.senderUid;
        const customerUid = ticket.senderUid;

        // Step 2: Determine who the recipient of the notification should be.
        let recipientUid;
        if (senderUid === customerUid) {
            // If the customer sent the reply, notify all admins.
            // For now, we'll just log this. A future version could notify a specific admin.
            console.log("Customer replied to a ticket. No admin notification is set up yet.");
            return;
        } else {
            // If the admin sent the reply, notify the customer.
            recipientUid = customerUid;
        }

        // Step 3: Create the in-app notification for the recipient.
        const notificationPayload = {
            userId: recipientUid,
            title: `New Reply from ${reply.senderName}`,
            message: reply.messageText,
            timestamp: Date.now(),
            isRead: false,
            // We can link this notification directly to the support ticket
            ticketId: ticketId,
        };
        await db.collection("users").doc(recipientUid).collection("notifications").add(notificationPayload);

        // Step 4: Send the push notification.
        const recipientDoc = await db.collection("users").doc(recipientUid).get();
        const fcmToken = recipientDoc.data()?.fcmToken;

        if (fcmToken) {
            const pushPayload = {
                notification: {
                    title: notificationPayload.title,
                    body: notificationPayload.message
                }
            };
            await admin.messaging().sendToDevice(fcmToken, pushPayload);
            console.log(`Push notification sent for ticket reply to user: ${recipientUid}`);
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
 * Triggers when a product order's status is updated. If the new status is
 * 'Ready for Pickup', it deducts the item quantities from the main product stock.
 * It also sends a low-stock notification to admins if necessary.
 */
exports.onOrderReadyForPickup = onDocumentUpdated("product_orders/{orderId}", async (event) => {
    const newData = event.data.after.data();
    const oldData = event.data.before.data();
    const db = admin.firestore();

    // Only run if the status changed specifically to 'Ready for Pickup'
    if (newData.status !== "Ready for Pickup" || oldData.status === "Ready for Pickup") {
        return null;
    }

    // --- FIX: ADDED CUSTOMER NOTIFICATION LOGIC ---
    const customerId = newData.customerId;
    if (!customerId) {
        console.error(`Order ${event.params.orderId} is missing a customerId!`);
    } else {
        const notificationPayload = {
            userId: customerId,
            title: "Your Order is Ready!",
            message: `Your order #${event.params.orderId.slice(0, 6)} is now ready for pickup at the salon.`,
            timestamp: Date.now(),
            isRead: false,
            orderId: event.params.orderId, // Link to the order
        };
        // 1. Create the in-app notification
        await db.collection("users").doc(customerId).collection("notifications").add(notificationPayload);

        // 2. Send the push notification
        const userDoc = await db.collection("users").doc(customerId).get();
        const fcmToken = userDoc.data()?.fcmToken;

        if (fcmToken) {
            const pushPayload = {
                notification: {
                    title: notificationPayload.title,
                    body: notificationPayload.message,
                    sound: "default"
                },
                data: { // Add orderId to data payload for potential deep linking in the app
                    orderId: event.params.orderId
                }
            };
            await admin.messaging().sendToDevice(fcmToken, pushPayload);
            console.log(`Sent "Ready for Pickup" push notification to user ${customerId}.`);
        }
    }
    // --- END OF FIX ---


    console.log(`Processing stock for order ${event.params.orderId}`);
    const items = newData.items;

    // Use a transaction for each item to safely update stock
    for (const item of items) {
        const productRef = db.collection("products").doc(item.productId);

        try {
            await db.runTransaction(async (transaction) => {
                const productDoc = await transaction.get(productRef);
                if (!productDoc.exists) {
                    throw `${item.productId} not found!`;
                }

                const productData = productDoc.data();
                const variants = productData.variants;
                let stockUpdated = false;

                const updatedVariants = variants.map(variant => {
                    if (variant.size === item.size) {
                        const newStock = variant.stock - item.quantity;
                        stockUpdated = true;

                        // Check for low stock (threshold is 5)
                        if (newStock <= 5 && variant.stock > 5) {
                            console.log(`LOW STOCK ALERT for ${productData.name} (${variant.size})`);
                            // This is where you would call a function to notify admins
                            notifyAdminsOfLowStock(productData, variant, newStock);
                        }

                        return { ...variant, stock: newStock };
                    }
                    return variant;
                });

                if (stockUpdated) {
                    transaction.update(productRef, { variants: updatedVariants });
                }
            });
        } catch (e) {
            console.error("Stock update transaction failed:", e);
        }
    }
    return null;
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
        // In the future, you could add a productId here to link directly to the product
    };

    // Send a notification to each admin
    for (const adminDoc of adminQuery.docs) {
        const adminUser = adminDoc.data();
        // 1. Create the in-app notification
        await db.collection("users").doc(adminUser.id).collection("notifications").add(notificationPayload);

        // 2. Send the push notification if they have a token
        if (adminUser.fcmToken) {
            const pushPayload = {
                notification: {
                    title: notificationPayload.title,
                    body: notificationPayload.message,
                },
            };
            await admin.messaging().sendToDevice(adminUser.fcmToken, pushPayload);
        }
    }
    console.log(`Sent low-stock alerts to ${adminQuery.size} admins.`);
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
            id: userId, name, email, phone, role: role.toUpperCase(), imageUrl: "",
        });
        return { message: `Successfully created user ${name} with role ${role}.` };
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
        .where("timestamp", "<", now)
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