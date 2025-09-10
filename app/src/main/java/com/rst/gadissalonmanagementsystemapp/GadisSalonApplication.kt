package com.rst.gadissalonmanagementsystemapp

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.initialize
import com.rst.gadissalonmanagementsystemapp.BuildConfig

class GadisSalonApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        Firebase.initialize(this)

        // --- INITIALIZE APP CHECK ---
        val appCheck = Firebase.appCheck

        if (BuildConfig.DEBUG) {
            // If this is a debug build, use the debug provider
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            // If this is a release build, use the real Play Integrity provider
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        // Read the saved theme preference as soon as the app starts
        val prefs = getSharedPreferences(Loading.PREFS_NAME, Context.MODE_PRIVATE)
        // Default to "Follow System" if no choice has been made
        val themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)

        // --- ENABLE FIRESTORE CACHING ---
        val firestore = Firebase.firestore
        val settings = firestoreSettings {
            isPersistenceEnabled = true
        }
        firestore.firestoreSettings = settings
    }


}