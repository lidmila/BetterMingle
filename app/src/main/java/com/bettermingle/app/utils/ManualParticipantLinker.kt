package com.bettermingle.app.utils

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object ManualParticipantLinker {

    suspend fun linkManualParticipant(
        eventId: String,
        manualParticipantId: String,
        realUserId: String
    ): Result<Unit> {
        val firestore = FirebaseFirestore.getInstance()
        val eventRef = firestore.collection("events").document(eventId)

        return try {
            // 1. Read the manual participant doc
            val manualDoc = eventRef.collection("participants")
                .document(manualParticipantId).get().await()
            val manualData = manualDoc.data ?: return Result.failure(Exception("Manual participant not found"))

            // 2. Read the real user profile
            val realUserDoc = firestore.collection("users").document(realUserId).get().await()
            val realDisplayName = realUserDoc.getString("displayName") ?: manualData["displayName"] as? String ?: ""
            val realAvatarUrl = realUserDoc.getString("avatarUrl") ?: ""

            // 3. Create new participant doc with real userId
            val newParticipantData = hashMapOf(
                "userId" to realUserId,
                "displayName" to realDisplayName,
                "avatarUrl" to realAvatarUrl,
                "role" to (manualData["role"] as? String ?: "PARTICIPANT"),
                "customRole" to (manualData["customRole"] as? String ?: ""),
                "rsvp" to (manualData["rsvp"] as? String ?: "ACCEPTED"),
                "isManual" to false,
                "linkedUserId" to null,
                "joinedAt" to (manualData["joinedAt"] ?: System.currentTimeMillis())
            )
            eventRef.collection("participants")
                .document(realUserId)
                .set(newParticipantData)
                .await()

            // 4. Delete the old manual participant doc
            eventRef.collection("participants")
                .document(manualParticipantId)
                .delete()
                .await()

            // 5. Migrate references in expenses (paidBy)
            val expenses = eventRef.collection("expenses").get().await()
            for (expDoc in expenses.documents) {
                if (expDoc.getString("paidBy") == manualParticipantId) {
                    expDoc.reference.update("paidBy", realUserId).await()
                }
                // Migrate splits
                val splits = expDoc.reference.collection("splits").get().await()
                for (splitDoc in splits.documents) {
                    if (splitDoc.getString("userId") == manualParticipantId) {
                        splitDoc.reference.update("userId", realUserId).await()
                    }
                }
            }

            // 6. Migrate references in tasks (assignedTo array)
            val tasks = eventRef.collection("tasks").get().await()
            for (taskDoc in tasks.documents) {
                @Suppress("UNCHECKED_CAST")
                val assignedTo = (taskDoc.get("assignedTo") as? List<String>) ?: emptyList()
                if (manualParticipantId in assignedTo) {
                    taskDoc.reference.update(
                        "assignedTo", FieldValue.arrayRemove(manualParticipantId)
                    ).await()
                    taskDoc.reference.update(
                        "assignedTo", FieldValue.arrayUnion(realUserId)
                    ).await()
                }
            }

            // 7. Migrate references in rooms (assignments array)
            val rooms = eventRef.collection("rooms").get().await()
            for (roomDoc in rooms.documents) {
                @Suppress("UNCHECKED_CAST")
                val assignments = (roomDoc.get("assignments") as? List<String>) ?: emptyList()
                if (manualParticipantId in assignments) {
                    roomDoc.reference.update(
                        "assignments", FieldValue.arrayRemove(manualParticipantId)
                    ).await()
                    roomDoc.reference.update(
                        "assignments", FieldValue.arrayUnion(realUserId)
                    ).await()
                }
            }

            // 8. Migrate wishlist claimedBy
            val wishlistItems = eventRef.collection("wishlistItems").get().await()
            for (doc in wishlistItems.documents) {
                if (doc.getString("claimedBy") == manualParticipantId) {
                    doc.reference.update("claimedBy", realUserId).await()
                }
            }

            // 9. Migrate packing items userId
            val packingItems = eventRef.collection("packingItems").get().await()
            for (doc in packingItems.documents) {
                if (doc.getString("userId") == manualParticipantId) {
                    doc.reference.update("userId", realUserId).await()
                }
            }

            Log.d("ManualParticipantLinker", "Successfully linked $manualParticipantId -> $realUserId in event $eventId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ManualParticipantLinker", "Failed to link participant", e)
            Result.failure(e)
        }
    }
}
