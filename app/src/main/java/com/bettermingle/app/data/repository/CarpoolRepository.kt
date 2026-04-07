package com.bettermingle.app.data.repository

import com.bettermingle.app.data.model.CarpoolPassenger
import com.bettermingle.app.data.model.CarpoolRide
import com.bettermingle.app.data.model.PassengerStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.bettermingle.app.utils.safeDocuments

class CarpoolRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getRidesByEvent(eventId: String): Flow<List<CarpoolRide>> = callbackFlow {
        val listener = firestore.collection("events").document(eventId)
            .collection("carpoolRides")
            .addSnapshotListener { snapshot, _ ->
                val rides = snapshot?.safeDocuments?.map { doc ->
                    documentToRide(eventId, doc.id, doc.data ?: emptyMap())
                } ?: emptyList()
                trySend(rides)
            }
        awaitClose { listener.remove() }
    }

    fun getPassengers(eventId: String, rideId: String): Flow<List<CarpoolPassenger>> = callbackFlow {
        val listener = firestore.collection("events").document(eventId)
            .collection("carpoolRides").document(rideId)
            .collection("passengers")
            .addSnapshotListener { snapshot, _ ->
                val passengers = snapshot?.safeDocuments?.map { doc ->
                    val data = doc.data ?: emptyMap()
                    CarpoolPassenger(
                        id = doc.id,
                        rideId = rideId,
                        userId = data["userId"] as? String ?: "",
                        displayName = data["displayName"] as? String ?: "",
                        status = try { PassengerStatus.valueOf(data["status"] as? String ?: "PENDING") }
                        catch (_: Exception) { PassengerStatus.PENDING },
                        pickupLocation = data["pickupLocation"] as? String ?: ""
                    )
                } ?: emptyList()
                trySend(passengers)
            }
        awaitClose { listener.remove() }
    }

    suspend fun offerRide(
        eventId: String,
        departureLocation: String,
        departureTime: Long?,
        availableSeats: Int,
        notes: String = ""
    ): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
        val rideId = UUID.randomUUID().toString()

        val rideData = mapOf(
            "driverId" to userId,
            "driverName" to (auth.currentUser?.displayName ?: ""),
            "departureLocation" to departureLocation,
            "departureTime" to departureTime,
            "availableSeats" to availableSeats,
            "notes" to notes,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("events").document(eventId)
            .collection("carpoolRides").document(rideId)
            .set(rideData).await()

        return rideId
    }

    suspend fun joinRide(eventId: String, rideId: String, pickupLocation: String = "") {
        val userId = auth.currentUser?.uid ?: return
        val passengerId = UUID.randomUUID().toString()

        val passengerData = mapOf(
            "userId" to userId,
            "displayName" to (auth.currentUser?.displayName ?: ""),
            "status" to PassengerStatus.PENDING.name,
            "pickupLocation" to pickupLocation
        )

        firestore.collection("events").document(eventId)
            .collection("carpoolRides").document(rideId)
            .collection("passengers").document(passengerId)
            .set(passengerData).await()
    }

    suspend fun approvePassenger(eventId: String, rideId: String, passengerId: String) {
        firestore.collection("events").document(eventId)
            .collection("carpoolRides").document(rideId)
            .collection("passengers").document(passengerId)
            .update("status", PassengerStatus.APPROVED.name).await()
    }

    suspend fun deleteRide(eventId: String, rideId: String) {
        firestore.collection("events").document(eventId)
            .collection("carpoolRides").document(rideId)
            .delete().await()
    }

    private fun documentToRide(eventId: String, id: String, data: Map<String, Any?>): CarpoolRide {
        return CarpoolRide(
            id = id,
            eventId = eventId,
            driverId = data["driverId"] as? String ?: "",
            driverName = data["driverName"] as? String ?: "",
            departureLocation = data["departureLocation"] as? String ?: "",
            departureLat = (data["departureLat"] as? Number)?.toDouble(),
            departureLng = (data["departureLng"] as? Number)?.toDouble(),
            departureTime = (data["departureTime"] as? Number)?.toLong(),
            availableSeats = (data["availableSeats"] as? Number)?.toInt() ?: 4,
            notes = data["notes"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
