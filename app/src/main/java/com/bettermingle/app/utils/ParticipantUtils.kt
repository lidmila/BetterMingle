package com.bettermingle.app.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

object ParticipantUtils {
    const val MANUAL_PREFIX = "manual_"

    fun isManualId(userId: String): Boolean = userId.startsWith(MANUAL_PREFIX)

    fun generateManualId(): String = "$MANUAL_PREFIX${UUID.randomUUID()}"

    suspend fun resolveDisplayName(
        userId: String,
        eventId: String,
        firestore: FirebaseFirestore
    ): String {
        if (isManualId(userId)) {
            return try {
                firestore.collection("events").document(eventId)
                    .collection("participants").document(userId)
                    .get().await()
                    .getString("displayName") ?: userId.take(8)
            } catch (_: Exception) { userId.take(8) }
        }
        return try {
            firestore.collection("users").document(userId)
                .get().await()
                .getString("displayName") ?: userId.take(8)
        } catch (_: Exception) { userId.take(8) }
    }
}
