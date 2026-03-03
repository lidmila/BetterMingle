package com.bettermingle.app.data.model

enum class PassengerStatus {
    PENDING, APPROVED, REJECTED
}

enum class CarpoolType {
    OFFER, REQUEST
}

data class CarpoolRide(
    val id: String = "",
    val eventId: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val departureLocation: String = "",
    val departureLat: Double? = null,
    val departureLng: Double? = null,
    val departureTime: Long? = null,
    val availableSeats: Int = 4,
    val notes: String = "",
    val type: CarpoolType = CarpoolType.OFFER,
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class CarpoolPassenger(
    val id: String = "",
    val rideId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val status: PassengerStatus = PassengerStatus.PENDING,
    val pickupLocation: String = ""
)
