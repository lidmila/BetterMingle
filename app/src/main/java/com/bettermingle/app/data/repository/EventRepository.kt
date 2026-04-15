package com.bettermingle.app.data.repository

import android.content.Context
import com.bettermingle.app.data.database.AppDatabase
import com.bettermingle.app.data.model.Event
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.data.model.EventStatus
import com.bettermingle.app.data.model.Participant
import com.bettermingle.app.data.model.ParticipantRole
import com.bettermingle.app.data.model.RsvpStatus
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.bettermingle.app.utils.safeDocuments

class EventRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val eventDao = db.eventDao()
    private val participantDao = db.participantDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()

    fun getActiveEvents(): Flow<List<Event>> = eventDao.getActiveEvents()

    fun getEventById(eventId: String): Flow<Event?> = eventDao.getEventById(eventId)

    fun getParticipantsByEvent(eventId: String): Flow<List<Participant>> =
        participantDao.getParticipantsByEvent(eventId)

    fun getParticipantCount(eventId: String): Flow<Int> =
        participantDao.getParticipantCount(eventId)

    suspend fun createEvent(
        name: String,
        description: String,
        theme: String = "",
        locationName: String,
        locationLat: Double? = null,
        locationLng: Double? = null,
        locationAddress: String = "",
        startDate: Long? = null,
        endDate: Long? = null,
        enabledModules: List<EventModule> = emptyList(),
        introText: String = "",
        securityEnabled: Boolean = false,
        eventPin: String = "",
        hideFinancials: Boolean = false,
        screenshotProtection: Boolean = false,
        autoDeleteDays: Int = 0,
        requireApproval: Boolean = false,
        invitedEmails: List<String> = emptyList()
    ): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
        val eventId = UUID.randomUUID().toString()
        val inviteCode = generateInviteCode()

        val event = Event(
            id = eventId,
            createdBy = userId,
            name = name,
            description = description,
            theme = theme,
            locationName = locationName,
            locationLat = locationLat,
            locationLng = locationLng,
            locationAddress = locationAddress,
            startDate = startDate,
            endDate = endDate,
            inviteCode = inviteCode,
            status = EventStatus.PLANNING,
            enabledModules = enabledModules,
            introText = introText,
            securityEnabled = securityEnabled,
            eventPin = eventPin,
            hideFinancials = hideFinancials,
            screenshotProtection = screenshotProtection,
            autoDeleteDays = autoDeleteDays,
            requireApproval = requireApproval,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Save locally
        eventDao.insertEvent(event)

        // Add creator as organizer (use userId as doc ID for Firestore rules compatibility)
        val participant = Participant(
            id = userId,
            eventId = eventId,
            userId = userId,
            displayName = auth.currentUser?.displayName ?: "",
            avatarUrl = auth.currentUser?.photoUrl?.toString() ?: "",
            role = ParticipantRole.ORGANIZER,
            rsvp = RsvpStatus.ACCEPTED,
            joinedAt = System.currentTimeMillis()
        )
        participantDao.insertParticipant(participant)

        // Sync to Firestore
        syncEventToCloud(event)
        syncParticipantToCloud(eventId, participant)

        // Store invited emails
        if (invitedEmails.isNotEmpty()) {
            try {
                firestore.collection("events").document(eventId)
                    .update("invitedEmails", invitedEmails)
            } catch (e: Exception) {
                Log.w("EventRepository", "Failed to store invited emails for $eventId", e)
            }
        }

        return eventId
    }

    suspend fun updateEvent(event: Event) {
        val updated = event.copy(updatedAt = System.currentTimeMillis())
        eventDao.updateEvent(updated)
        syncEventToCloud(updated)
    }

    suspend fun deleteEvent(eventId: String) {
        eventDao.deleteEventById(eventId)
        try {
            val eventRef = firestore.collection("events").document(eventId)
            val subcollections = listOf(
                "participants", "polls", "expenses", "messages", "carpoolRides",
                "rooms", "schedule", "tasks", "packingItems", "wishlistItems", "lastSeen"
            )
            // Fetch all subcollections in parallel, then batch-delete
            val results = coroutineScope {
                subcollections.map { sub ->
                    async {
                        try {
                            eventRef.collection(sub).get().await().safeDocuments
                        } catch (e: Exception) {
                            Log.w("EventRepository", "Failed to fetch subcollection $sub for $eventId", e)
                            emptyList<com.google.firebase.firestore.DocumentSnapshot>()
                        }
                    }
                }.awaitAll()
            }
            val allDocs = results.flatten()
            for (chunk in allDocs.chunked(500)) {
                val batch = firestore.batch()
                for (doc in chunk) { batch.delete(doc.reference) }
                batch.commit().await()
            }
            eventRef.delete().await()
        } catch (e: Exception) {
            Log.e("EventRepository", "Failed to delete event $eventId from cloud", e)
        }
    }

    suspend fun syncEventToCloud(event: Event) {
        try {
            val eventData = mapOf(
                "createdBy" to event.createdBy,
                "name" to event.name,
                "description" to event.description,
                "theme" to event.theme,
                "templateSlug" to event.templateSlug,
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
                @Suppress("USELESS_ELVIS")
                "enabledModules" to (event.enabledModules ?: emptyList()).map { it.name },
                "securityEnabled" to event.securityEnabled,
                "eventPin" to event.eventPin,
                "hideFinancials" to event.hideFinancials,
                "screenshotProtection" to event.screenshotProtection,
                "autoDeleteDays" to event.autoDeleteDays,
                "requireApproval" to event.requireApproval,
                "introText" to event.introText,
                "moduleColors" to event.moduleColors,
                "createdAt" to event.createdAt,
                "updatedAt" to event.updatedAt
            )
            firestore.collection("events")
                .document(event.id)
                .set(eventData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("EventRepository", "Failed to sync event ${event.id} to cloud", e)
        }
    }

    private suspend fun syncParticipantToCloud(eventId: String, participant: Participant) {
        try {
            val data = mapOf(
                "userId" to participant.userId,
                "displayName" to participant.displayName,
                "avatarUrl" to participant.avatarUrl,
                "role" to participant.role.name,
                "rsvp" to participant.rsvp.name,
                "joinedAt" to participant.joinedAt,
                "isManual" to participant.isManual,
                "linkedUserId" to participant.linkedUserId
            )
            firestore.collection("events")
                .document(eventId)
                .collection("participants")
                .document(participant.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w("EventRepository", "Failed to sync participant ${participant.id} to cloud", e)
        }
    }

    suspend fun syncFromCloud() {
        val userId = auth.currentUser?.uid ?: run {
            Log.w("EventRepository", "syncFromCloud: no authenticated user")
            return
        }
        Log.d("EventRepository", "syncFromCloud: starting for user $userId")
        val syncedEventIds = mutableSetOf<String>()

        // 1. Fetch events where user is creator
        try {
            val ownedEvents = firestore.collection("events")
                .whereEqualTo("createdBy", userId)
                .get()
                .await()

            Log.d("EventRepository", "syncFromCloud: found ${ownedEvents.size()} owned events")
            for (doc in ownedEvents.safeDocuments) {
                val event = documentToEvent(doc.id, doc.data ?: continue)
                eventDao.insertEvent(event)
                syncedEventIds.add(doc.id)
                syncParticipantsFromCloud(doc.id)
            }
        } catch (e: Exception) {
            Log.e("EventRepository", "Sync owned events failed: ${e.message}", e)
        }

        // 2. Find events where user is a participant via collection group query
        try {
            val participantDocs = firestore.collectionGroup("participants")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (participantDoc in participantDocs.safeDocuments) {
                // Path: events/{eventId}/participants/{participantId}
                val eventRef = participantDoc.reference.parent.parent ?: continue
                val eventId = eventRef.id
                if (eventId in syncedEventIds) continue

                try {
                    val eventDoc = eventRef.get().await()
                    if (eventDoc.exists()) {
                        val event = documentToEvent(eventDoc.id, eventDoc.data ?: continue)
                        eventDao.insertEvent(event)
                        syncedEventIds.add(eventId)
                        syncParticipantsFromCloud(eventId)
                    }
                } catch (e: Exception) {
                    Log.e("EventRepository", "Sync event $eventId failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e("EventRepository", "Sync participant events failed: ${e.message}", e)
        }

        // 3. Remove local events that no longer exist in Firestore
        try {
            val localEvents = eventDao.getAllEventsOnce()
            for (local in localEvents) {
                if (local.id !in syncedEventIds) {
                    eventDao.deleteEventById(local.id)
                    Log.d("EventRepository", "syncFromCloud: removed stale local event ${local.id}")
                }
            }
        } catch (e: Exception) {
            Log.e("EventRepository", "Cleanup stale events failed: ${e.message}", e)
        }

        Log.d("EventRepository", "Synced ${syncedEventIds.size} events total")
    }

    private suspend fun syncParticipantsFromCloud(eventId: String) {
        try {
            val participants = firestore.collection("events")
                .document(eventId)
                .collection("participants")
                .get()
                .await()

            for (doc in participants.safeDocuments) {
                val data = doc.data ?: continue
                val role = try {
                    ParticipantRole.valueOf((data["role"] as? String ?: "PARTICIPANT").uppercase())
                } catch (_: Exception) { ParticipantRole.PARTICIPANT }
                val rsvp = try {
                    RsvpStatus.valueOf((data["rsvp"] as? String ?: "PENDING").uppercase())
                } catch (_: Exception) { RsvpStatus.PENDING }

                val participant = Participant(
                    id = doc.id,
                    eventId = eventId,
                    userId = data["userId"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: "",
                    avatarUrl = data["avatarUrl"] as? String ?: "",
                    role = role,
                    rsvp = rsvp,
                    joinedAt = (data["joinedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isManual = data["isManual"] as? Boolean ?: false,
                    linkedUserId = data["linkedUserId"] as? String
                )
                participantDao.insertParticipant(participant)
            }
        } catch (e: Exception) {
            Log.w("EventRepository", "Failed to sync participants from cloud for event $eventId", e)
        }
    }

    private fun documentToEvent(id: String, data: Map<String, Any?>): Event {
        @Suppress("UNCHECKED_CAST")
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
            theme = data["theme"] as? String ?: "",
            templateSlug = data["templateSlug"] as? String ?: "",
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
            introText = data["introText"] as? String ?: "",
            moduleColors = (data["moduleColors"] as? Map<*, *>)
                ?.entries
                ?.mapNotNull { (k, v) -> if (k is String && v is String) k to v else null }
                ?.toMap() ?: emptyMap(),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    suspend fun updateModuleColor(eventId: String, moduleName: String, colorHex: String) {
        try {
            firestore.collection("events").document(eventId)
                .update("moduleColors.$moduleName", colorHex).await()
            val event = eventDao.getEventByIdOnce(eventId) ?: return
            eventDao.updateEvent(event.copy(
                moduleColors = event.moduleColors + (moduleName to colorHex),
                updatedAt = System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            Log.e("EventRepository", "Failed to update module color for $eventId/$moduleName", e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
