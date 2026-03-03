package com.bettermingle.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ParticipantRole {
    ORGANIZER, CO_ORGANIZER, PARTICIPANT
}

enum class RsvpStatus {
    PENDING, ACCEPTED, DECLINED, MAYBE
}

@Entity(tableName = "participants")
data class Participant(
    @PrimaryKey val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val role: ParticipantRole = ParticipantRole.PARTICIPANT,
    val rsvp: RsvpStatus = RsvpStatus.PENDING,
    val joinedAt: Long = System.currentTimeMillis()
)
