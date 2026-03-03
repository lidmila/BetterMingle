package com.bettermingle.app.data.repository

import com.bettermingle.app.data.model.EventRoom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RoomRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getRoomsByEvent(eventId: String): Flow<List<EventRoom>> = callbackFlow {
        val listener = firestore.collection("events").document(eventId)
            .collection("rooms")
            .addSnapshotListener { snapshot, _ ->
                val rooms = snapshot?.documents?.map { doc ->
                    documentToRoom(eventId, doc.id, doc.data ?: emptyMap())
                } ?: emptyList()
                trySend(rooms)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createRoom(
        eventId: String,
        name: String,
        capacity: Int = 2,
        notes: String = ""
    ): String {
        val roomId = UUID.randomUUID().toString()

        val roomData = mapOf(
            "name" to name,
            "capacity" to capacity,
            "notes" to notes,
            "assignments" to emptyList<String>()
        )

        firestore.collection("events").document(eventId)
            .collection("rooms").document(roomId)
            .set(roomData).await()

        return roomId
    }

    suspend fun assignToRoom(eventId: String, roomId: String, userId: String) {
        firestore.collection("events").document(eventId)
            .collection("rooms").document(roomId)
            .update("assignments", FieldValue.arrayUnion(userId)).await()
    }

    suspend fun removeFromRoom(eventId: String, roomId: String, userId: String) {
        firestore.collection("events").document(eventId)
            .collection("rooms").document(roomId)
            .update("assignments", FieldValue.arrayRemove(userId)).await()
    }

    suspend fun deleteRoom(eventId: String, roomId: String) {
        firestore.collection("events").document(eventId)
            .collection("rooms").document(roomId)
            .delete().await()
    }

    @Suppress("UNCHECKED_CAST")
    private fun documentToRoom(eventId: String, id: String, data: Map<String, Any?>): EventRoom {
        return EventRoom(
            id = id,
            eventId = eventId,
            name = data["name"] as? String ?: "",
            capacity = (data["capacity"] as? Number)?.toInt() ?: 2,
            notes = data["notes"] as? String ?: "",
            assignments = (data["assignments"] as? List<String>) ?: emptyList()
        )
    }
}
