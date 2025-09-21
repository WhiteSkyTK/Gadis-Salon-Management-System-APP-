// index.js

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

    // Only trigger if the status has actually changed
    if (newData.status === oldData.status) {
        console.log("Status unchanged, no notification sent.");
        return;
    }

    const customerId = newData.customerId;
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
    };
    await admin.firestore().collection("users").doc(customerId).collection("notifications").add(notificationPayload);

    const userDoc = await admin.firestore().collection("users").doc(customerId).get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (fcmToken) {
        const pushPayload = {
            notification: {
                title: notificationPayload.title,
                body: notificationPayload.message,
            },
        };
        await admin.messaging().sendToDevice(fcmToken, pushPayload);
        console.log("Push notification sent to customer:", customerId);
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
        const pushPayload = { notification: { title: notificationPayload.title, body: notificationPayload.message } };
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
        console.log("Sending push notification to admin tokens:", tokens);
        return admin.messaging().sendToDevice(tokens, payload);
    }
    console.log("No admin tokens found to send notification.");
    return;
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
 * v2 Callable function that allows an admin to set a custom role
 * on another user's account.
 */

exports.setUserRole = onCall(async(request) => {
    // This is the new, alternative way to check for an admin.
    // It's safer from auto-formatting errors.

    if (!request.auth || !request.auth.token || request.auth.token.admin !== true) {
        throw new HttpsError(
            "permission-denied",
            "Request not authorized. User must be an admin to set roles.",
        );
    }

    const {
        userId,
        role
    } = request.data;
    const lowerCaseRole = role.toLowerCase();
    const upperCaseRole = role.toUpperCase();

    try {
        // Set custom claims for auth role
        await admin.auth().setCustomUserClaims(userId, {
            [lowerCaseRole]: true
        });

        // Update the role in Firestore for easy querying
        await admin.firestore().collection("users").doc(userId).update({
            role: upperCaseRole,
        });

        return {
            message: `Success! User ${userId} has been made a ${upperCaseRole}.`,
        };
    } catch (error) {
        console.error("Error setting user role:", error);
        throw new HttpsError("internal", error.message);
    }
});

/**
 * v2 Callable function that allows an admin to delete a user
 * from both Authentication and Firestore.
 */
exports.deleteUser = onCall(async (request) => {
    if (!request.auth || !request.auth.token || request.auth.token.admin !== true) {
        throw new HttpsError(
            "permission-denied",
            "Request not authorized. User must be an admin.",
        );
    }
    const userId = request.data.userId;
    try {
        // Delete from Firebase Authentication
        await admin.auth().deleteUser(userId);
        // Delete from Firestore
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