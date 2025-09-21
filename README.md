# Gadis Salon Management System

Gadis Salon is a comprehensive, modern salon management application developed as a final-year project for Rosebank College. This native Android application provides a seamless, real-time experience for customers, stylists (workers), and administrators, bringing the entire salon workflow into a single, cohesive digital ecosystem powered by Google Firebase.

## 👥 Project Team: RST Innovations

**Team Members:**

- **Tokollo Nonyane (ST10296818)**
- **Sagwadi Mashimbye (ST10168528)**
- **Rinae Magadagela (ST10361117)**

## 🌟 Project Overview

This application serves three distinct user roles, each with a tailored portal to manage their specific needs. From customer bookings and product purchases to worker schedules and admin oversight, the Gadis Salon app is designed to be a one-stop solution for a modern salon. The app is built on a robust backend using Google Firebase, ensuring real-time data synchronization across all user types and a future-proof foundation for integration with a web-based platform.

## ✨ Key Features

### 👤 Customer Portal

- **Secure Authentication:** Users can register and log in securely using Email/Password or their Google account.
- **Product & Hairstyle Browsing:** A clean, intuitive interface to explore all available salon products and hairstyle services.
- **Favorites & Shopping Cart:** Users can save favorite items and manage a cart for product purchases.
- **Seamless Booking System:** A multi-step process to book hairstyle appointments with available stylists, select dates, and choose times.
- **Order & Booking History:** Dedicated sections in the profile to view past product orders and service bookings.
- **Live Support System:** Users can submit support tickets and view admin replies.

### ✂️ Worker / Stylist Portal

- **Secure Role-Based Login:** Workers log in with dedicated accounts managed by an admin.
- **New Booking Management:** A real-time list of pending booking requests with the ability to "Accept" or "Decline".
- **Personal Schedule:** An interactive calendar showing all confirmed appointments for the logged-in stylist.
- **Inventory Management:** A view-only list of salon products and their current stock levels.
- **In-App Messaging:** A dedicated chat system linked to each booking for direct communication with the customer.

### 🔐 Admin Portal

- **Secure Admin Login:** Admins log in with a privileged account with full access rights.
- **Live Dashboard:** A summary screen showing key metrics like customer counts, stylist counts, and total bookings.
- **Full User Management (CRUD):** Admins can create, view, update, and delete all user accounts (Customers, Workers, and other Admins) and securely assign roles.
- **Product & Hairstyle Management (CRUD):** Full control to add new products and hairstyles, upload images, set prices, and manage stock levels.
- **Booking & Order Oversight:** A complete view of all bookings and product orders in the system.
- **Live Support Ticket System:** An inbox to view, manage, and reply to all support messages.
- **Dynamic Content Management:** Admins can edit app-wide content like the "About Us" page and the salon's location on the map.

## 🛠️ Technology Stack

- **Frontend:** Native Android (Kotlin)
- **Architecture:** MVVM (Model-View-ViewModel) with a Repository Pattern (`FirebaseManager`)
- **UI:** Material Design 3, XML Layouts with ViewBinding
- **Navigation:** Android Jetpack Navigation Component
- **Backend:** Google Firebase
    - **Authentication:** For secure user login, role management, and Google Sign-In.
    - **Cloud Firestore:** As the real-time NoSQL database for all app data.
    - **Firebase Storage:** For hosting all image assets (profiles, products, hairstyles).
    - **Cloud Functions:** For backend logic like sending email/push notifications and setting user roles.
    - **App Check:** For enhanced security against unauthorized clients.
- **APIs:** Google Maps SDK, Google Places SDK
- **Image Loading:** Coil

## 🚀 Getting Started

### Prerequisites

- Android Studio Iguana | 2023.2.1 or later
- A Google Firebase project
- A Google Maps API Key

### Build & Run Steps

1. **Clone the repository:**

   ```bash
      git clone [YOUR_REPOSITORY_URL_HERE]
   
2. **Open in Android Studio:** Open the cloned folder as a new project in Android Studio.

3. **Firebase Setup:**

- Follow the Firebase setup assistant **(Tools > Firebase)** to connect the app to your Firebase project.

- This will automatically download and add your project's (`google-services.json`) file to the (`app/`) directory.

4. **API Keys & Credentials:**

- In the root directory of the project, create a file named (`local.properties`)
- add the following (this file is ignored by Git for security):

```bash
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

5. **Build & Run the App:** Let Gradle sync.

Build and run the app on an emulator or connected device.

## Installation & Testing
1. **Enable Firebase Services:** In your Firebase console, ensure the following are enabled:
- Authentication: (Email/Password and Google)
- Cloud Firestore 
- Firebase Storage 
- App Check

2. **Create First Admin User:** Use the provided Node.js script (`setAdmin.js`) to manually promote a user to an admin.

Refer to the /scripts directory or the documentation for setup instructions.

3. **App Check on Emulator:** Get the debug token from Logcat during testing and add it under App Check > Debug Providers in your Firebase console.

# 📺 Project Showcase
YouTube Demo: [LINK_TO_YOUTUBE_VIDEO_HERE]

Download APK: [LINK_TO_RELEASE_APK_HERE]

# 📸 Screenshots
(Add screenshots here once available. Use GitHub image uploads or hosted URLs.)

Login & Register	Customer Home	Product Detail

Admin Dashboard	Worker Schedule	Booking Confirmation

# 🙏 Acknowledgements
This project was developed as a final-year submission for **Rosebank College**. We would like to extend our sincere gratitude to the **JB Marks Education Trust Fund** for their invaluable support through the bursary program, which made our studies and this project possible.
