package com.bettermingle.app.data.database

import androidx.room.TypeConverter
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.data.model.EventStatus
import com.bettermingle.app.data.model.ParticipantRole
import com.bettermingle.app.data.model.RsvpStatus

class Converters {
    @TypeConverter
    fun fromEventModuleList(value: List<EventModule>): String {
        return value.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toEventModuleList(value: String): List<EventModule> {
        if (value.isBlank()) return emptyList()
        return value.split(",").mapNotNull {
            try { EventModule.valueOf(it) } catch (_: IllegalArgumentException) { null }
        }
    }

    @TypeConverter
    fun fromEventStatus(value: EventStatus): String = value.name

    @TypeConverter
    fun toEventStatus(value: String): EventStatus = EventStatus.valueOf(value)

    @TypeConverter
    fun fromParticipantRole(value: ParticipantRole): String = value.name

    @TypeConverter
    fun toParticipantRole(value: String): ParticipantRole = ParticipantRole.valueOf(value)

    @TypeConverter
    fun fromRsvpStatus(value: RsvpStatus): String = value.name

    @TypeConverter
    fun toRsvpStatus(value: String): RsvpStatus = RsvpStatus.valueOf(value)
}
