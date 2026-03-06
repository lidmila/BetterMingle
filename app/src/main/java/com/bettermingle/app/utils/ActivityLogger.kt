package com.bettermingle.app.utils

import android.content.Context
import android.util.Log
import com.bettermingle.app.data.preferences.SettingsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

object ActivityLogger {

    private var settingsManager: SettingsManager? = null
    private val pendingActivities = mutableListOf<PendingActivity>()
    private val bufferLock = Any()
    private val eventNameCache = mutableMapOf<String, String>()

    private data class PendingActivity(
        val eventId: String,
        val type: String,
        val description: String,
        val eventName: String,
        val data: Map<String, Any>
    )

    fun initialize(context: Context) {
        settingsManager = SettingsManager(context.applicationContext)
        // Flush any buffered activities
        synchronized(bufferLock) {
            val pending = pendingActivities.toList()
            pendingActivities.clear()
            if (pending.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    for (p in pending) {
                        val resolvedName = p.eventName.ifEmpty { resolveEventName(p.eventId) }
                        saveLocally(p.eventId, p.type, p.description, p.data, resolvedName)
                        saveToFirestore(p.eventId, p.data)
                    }
                }
            }
        }
    }

    fun log(
        eventId: String,
        type: String,
        description: String,
        eventName: String = ""
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val data = mapOf(
            "actorId" to user.uid,
            "actorName" to (user.displayName ?: user.email?.substringBefore("@") ?: ""),
            "type" to type,
            "description" to description,
            "timestamp" to System.currentTimeMillis(),
            "readBy" to listOf(user.uid)
        )

        // Cache event name if provided
        if (eventName.isNotEmpty()) {
            eventNameCache[eventId] = eventName
        }

        val sm = settingsManager
        if (sm != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val resolvedName = eventName.ifEmpty { resolveEventName(eventId) }
                saveLocally(eventId, type, description, data, resolvedName)
                saveToFirestore(eventId, data)
            }
        } else {
            synchronized(bufferLock) {
                pendingActivities.add(PendingActivity(eventId, type, description, eventName, data))
            }
            saveToFirestore(eventId, data)
        }
    }

    private suspend fun resolveEventName(eventId: String): String {
        // Check cache first
        eventNameCache[eventId]?.let { return it }
        // Fetch from Firestore
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .get().await()
            val name = doc.getString("name") ?: ""
            if (name.isNotEmpty()) eventNameCache[eventId] = name
            name
        } catch (e: Exception) {
            Log.e("ActivityLogger", "Failed to resolve event name for $eventId: ${e.message}")
            ""
        }
    }

    private suspend fun saveLocally(
        eventId: String,
        type: String,
        description: String,
        data: Map<String, Any>,
        eventName: String = ""
    ) {
        try {
            val sm = settingsManager ?: return
            sm.addUserActivity(
                SettingsManager.LocalActivityEntry(
                    id = UUID.randomUUID().toString(),
                    eventId = eventId,
                    eventName = eventName,
                    actorName = data["actorName"] as? String ?: "",
                    actorId = data["actorId"] as? String ?: "",
                    type = type,
                    description = description,
                    timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis()
                )
            )
            sm.incrementUnreadActivityCount(1)
        } catch (e: Exception) {
            Log.e("ActivityLogger", "Failed to save activity locally: ${e.message}")
        }
    }

    private fun saveToFirestore(eventId: String, data: Map<String, Any>) {
        FirebaseFirestore.getInstance()
            .collection("events").document(eventId)
            .collection("activity")
            .add(data)
            .addOnFailureListener { e ->
                Log.e("ActivityLogger", "Failed to log activity to Firestore: ${e.message}")
            }
    }
}
