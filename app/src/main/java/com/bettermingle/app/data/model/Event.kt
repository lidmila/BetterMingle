package com.bettermingle.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventStatus {
    PLANNING, CONFIRMED, ONGOING, COMPLETED, CANCELLED
}

enum class EventModule {
    VOTING, EXPENSES, CARPOOL, ROOMS, CHAT, SCHEDULE, TASKS, PACKING_LIST, WISHLIST, CATERING, BUDGET
}

val PREDEFINED_THEMES = listOf(
    "Svatba", "Narozeniny", "Teambuilding", "Firemní akce",
    "Výlet", "Festival", "Oslava", "Sport", "Rozlučka", "Vánoční večírek"
)

@Entity(tableName = "events")
data class Event(
    @PrimaryKey val id: String = "",
    val createdBy: String = "",
    val name: String = "",
    val description: String = "",
    val theme: String = "",
    val templateSlug: String = "",
    val locationName: String = "",
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationAddress: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val datesFinalized: Boolean = false,
    val coverImageUrl: String = "",
    val inviteCode: String = "",
    val maxParticipants: Int = 0,
    val status: EventStatus = EventStatus.PLANNING,
    val enabledModules: List<EventModule> = emptyList(),
    // Security settings
    val securityEnabled: Boolean = false,
    val eventPin: String = "",
    val hideFinancials: Boolean = false,
    val screenshotProtection: Boolean = false,
    val autoDeleteDays: Int = 0, // 0 = disabled, otherwise days after endDate
    val requireApproval: Boolean = false, // organizer must approve new participants
    val introText: String = "",
    val moduleColors: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
