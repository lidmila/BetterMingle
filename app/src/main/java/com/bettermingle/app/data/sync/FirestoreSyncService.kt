package com.bettermingle.app.data.sync

import android.content.Context
import android.util.Log
import com.bettermingle.app.data.database.AppDatabase
import com.bettermingle.app.data.model.Event
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.data.model.EventStatus
import com.bettermingle.app.data.model.Participant
import com.bettermingle.app.data.model.ParticipantRole
import com.bettermingle.app.data.model.RsvpStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Handles bidirectional sync between local Room database and Firestore.
 * Local changes are pushed to Firestore, remote changes are pulled to Room.
 */
class FirestoreSyncService(context: Context) {
    companion object {
        private const val TAG = "FirestoreSyncService"
    }

    private val db = AppDatabase.getDatabase(context)
    private val eventDao = db.eventDao()
    private val participantDao = db.participantDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Pull all events the user participates in from Firestore to Room.
     */
    suspend fun pullEvents() {
        val userId = auth.currentUser?.uid ?: return

        try {
            // Events created by user
            val ownedDocs = firestore.collection("events")
                .whereEqualTo("createdBy", userId)
                .get().await()

            val remoteEventIds = ownedDocs.documents.mapNotNull { it.id }.toSet()

            for (doc in ownedDocs.documents) {
                val event = documentToEvent(doc.id, doc.data ?: continue)
                eventDao.insertEvent(event)
                pullParticipants(doc.id)
            }

            // Delete local events that no longer exist in Firestore
            val localEvents = eventDao.getAllEventsOnce()
            for (localEvent in localEvents) {
                if (localEvent.createdBy == userId && localEvent.id !in remoteEventIds) {
                    participantDao.deleteAllByEvent(localEvent.id)
                    eventDao.deleteEventById(localEvent.id)
                    Log.d(TAG, "Deleted orphaned local event ${localEvent.id}")
                }
            }

            Log.d(TAG, "Pulled ${ownedDocs.size()} owned events")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull events", e)
        }
    }

    /**
     * Pull participants for a specific event.
     */
    suspend fun pullParticipants(eventId: String) {
        try {
            val docs = firestore.collection("events").document(eventId)
                .collection("participants").get().await()

            for (doc in docs.documents) {
                val data = doc.data ?: continue
                val participant = Participant(
                    id = doc.id,
                    eventId = eventId,
                    userId = data["userId"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: "",
                    avatarUrl = data["avatarUrl"] as? String ?: "",
                    role = try { ParticipantRole.valueOf(data["role"] as? String ?: "PARTICIPANT") }
                    catch (_: Exception) { ParticipantRole.PARTICIPANT },
                    rsvp = try { RsvpStatus.valueOf(data["rsvp"] as? String ?: "PENDING") }
                    catch (_: Exception) { RsvpStatus.PENDING },
                    joinedAt = (data["joinedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isManual = data["isManual"] as? Boolean ?: false,
                    linkedUserId = data["linkedUserId"] as? String
                )
                participantDao.insertParticipant(participant)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull participants for $eventId", e)
        }
    }

    /**
     * Push a local event to Firestore.
     */
    suspend fun pushEvent(event: Event) {
        try {
            val data = eventToMap(event)
            firestore.collection("events")
                .document(event.id)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .await()
            Log.d(TAG, "Pushed event ${event.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push event ${event.id}", e)
        }
    }

    /**
     * Delete remote event and all subcollections.
     */
    suspend fun deleteRemoteEvent(eventId: String) {
        try {
            // Delete subcollections first
            val subcollections = listOf(
                "participants", "polls", "expenses", "carpoolRides",
                "rooms", "schedule", "messages", "packingItems",
                "photos", "ratings"
            )

            for (sub in subcollections) {
                val docs = firestore.collection("events").document(eventId)
                    .collection(sub).get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            }

            firestore.collection("events").document(eventId).delete().await()
            Log.d(TAG, "Deleted remote event $eventId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete remote event $eventId", e)
        }
    }

    private fun eventToMap(event: Event): Map<String, Any?> = mapOf(
        "createdBy" to event.createdBy,
        "name" to event.name,
        "description" to event.description,
        "locationName" to event.locationName,
        "locationLat" to event.locationLat,
        "locationLng" to event.locationLng,
        "locationAddress" to event.locationAddress,
        "startDate" to event.startDate,
        "endDate" to event.endDate,
        "datesFinalized" to event.datesFinalized,
        "coverImageUrl" to event.coverImageUrl,
        "inviteCode" to event.inviteCode,
        "maxParticipants" to event.maxParticipants,
        "status" to event.status.name,
        "enabledModules" to event.enabledModules.map { it.name },
        "securityEnabled" to event.securityEnabled,
        "eventPin" to event.eventPin,
        "hideFinancials" to event.hideFinancials,
        "screenshotProtection" to event.screenshotProtection,
        "autoDeleteDays" to event.autoDeleteDays,
        "requireApproval" to event.requireApproval,
        "createdAt" to event.createdAt,
        "updatedAt" to event.updatedAt
    )

    @Suppress("UNCHECKED_CAST")
    private fun documentToEvent(id: String, data: Map<String, Any?>): Event {
        val moduleNames = (data["enabledModules"] as? List<String>) ?: emptyList()
        val modules = moduleNames.mapNotNull {
            try { EventModule.valueOf(it) } catch (_: Exception) { null }
        }
        val status = try {
            EventStatus.valueOf(data["status"] as? String ?: "PLANNING")
        } catch (_: Exception) { EventStatus.PLANNING }

        return Event(
            id = id,
            createdBy = data["createdBy"] as? String ?: "",
            name = data["name"] as? String ?: "",
            description = data["description"] as? String ?: "",
            locationName = data["locationName"] as? String ?: "",
            locationLat = (data["locationLat"] as? Number)?.toDouble(),
            locationLng = (data["locationLng"] as? Number)?.toDouble(),
            locationAddress = data["locationAddress"] as? String ?: "",
            startDate = (data["startDate"] as? Number)?.toLong(),
            endDate = (data["endDate"] as? Number)?.toLong(),
            datesFinalized = data["datesFinalized"] as? Boolean ?: false,
            coverImageUrl = data["coverImageUrl"] as? String ?: "",
            inviteCode = data["inviteCode"] as? String ?: "",
            maxParticipants = (data["maxParticipants"] as? Number)?.toInt() ?: 0,
            status = status,
            enabledModules = modules,
            securityEnabled = data["securityEnabled"] as? Boolean ?: false,
            eventPin = data["eventPin"] as? String ?: "",
            hideFinancials = data["hideFinancials"] as? Boolean ?: false,
            screenshotProtection = data["screenshotProtection"] as? Boolean ?: false,
            autoDeleteDays = (data["autoDeleteDays"] as? Number)?.toInt() ?: 0,
            requireApproval = data["requireApproval"] as? Boolean ?: false,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
