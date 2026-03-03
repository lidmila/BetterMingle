package com.bettermingle.app

import android.app.Application
import com.bettermingle.app.notification.NotificationChannels
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp

class BetterMingleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificationChannels.createAll(this)

        if (!Places.isInitialized()) {
            val apiKey = packageManager
                .getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
                .metaData
                ?.getString("com.google.android.geo.API_KEY") ?: ""
            if (apiKey.isNotBlank()) {
                Places.initializeWithNewPlacesApiEnabled(this, apiKey)
            }
        }
    }
}
