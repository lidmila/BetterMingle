package com.bettermingle.app

import android.app.Application
import android.util.Log
import com.bettermingle.app.notification.NotificationChannels
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp

class BetterMingleApp : Application() {

    companion object {
        private const val TAG = "BetterMingleApp"
        var placesInitMode: PlacesInitMode = PlacesInitMode.NONE
            private set
    }

    enum class PlacesInitMode { NEW_API, LEGACY_API, NONE }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificationChannels.createAll(this)
        initializePlaces()
    }

    private fun initializePlaces() {
        if (Places.isInitialized()) {
            Log.d(TAG, "Places already initialized")
            return
        }

        val apiKey = try {
            packageManager
                .getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
                .metaData
                ?.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read API key from manifest", e)
            ""
        }

        if (apiKey.isBlank()) {
            Log.w(TAG, "Places API key is blank — skipping initialization")
            return
        }

        Log.d(TAG, "API key loaded (length=${apiKey.length}), initializing Places SDK…")

        // Try New Places API first, fall back to legacy
        try {
            Places.initializeWithNewPlacesApiEnabled(this, apiKey)
            placesInitMode = PlacesInitMode.NEW_API
            Log.i(TAG, "Places SDK initialized with NEW API")
        } catch (e: Exception) {
            Log.w(TAG, "New Places API init failed, trying legacy…", e)
            try {
                Places.initialize(this, apiKey)
                placesInitMode = PlacesInitMode.LEGACY_API
                Log.i(TAG, "Places SDK initialized with LEGACY API")
            } catch (e2: Exception) {
                Log.e(TAG, "Places SDK initialization failed completely", e2)
                placesInitMode = PlacesInitMode.NONE
            }
        }
    }
}
