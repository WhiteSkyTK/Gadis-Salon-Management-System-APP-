package com.rst.gadissalonmanagementsystemapp

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class GadisSalonApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Read the saved theme preference as soon as the app starts
        val prefs = getSharedPreferences(Loading.PREFS_NAME, Context.MODE_PRIVATE)
        val themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
}