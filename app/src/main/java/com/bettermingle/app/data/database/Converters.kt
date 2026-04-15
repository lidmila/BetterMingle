package com.bettermingle.app.data.database

import androidx.room.TypeConverter
import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.data.model.EventStatus
import com.bettermingle.app.data.model.ParticipantRole
import com.bettermingle.app.data.model.RsvpStatus

class Converters {
    @TypeConverter
    fun fromEventModuleList(value: List<EventModule>?): String {
        return value?.joinToString(",") { it.name } ?: ""
    }

    @TypeConverter
    fun toEventModuleList(value: String?): List<EventModule> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull {
            try { EventModule.valueOf(it) } catch (_: IllegalArgumentException) { null }
        }
    }

    @TypeConverter
    fun fromEventStatus(value: EventStatus): String = value.name

    @TypeConverter
    fun toEventStatus(value: String?): EventStatus =
        value?.let { try { EventStatus.valueOf(it) } catch (_: Exception) { EventStatus.PLANNING } }
            ?: EventStatus.PLANNING

    @TypeConverter
    fun fromParticipantRole(value: ParticipantRole): String = value.name

    @TypeConverter
    fun toParticipantRole(value: String?): ParticipantRole =
        value?.let { try { ParticipantRole.valueOf(it) } catch (_: Exception) { ParticipantRole.PARTICIPANT } }
            ?: ParticipantRole.PARTICIPANT

    @TypeConverter
    fun fromRsvpStatus(value: RsvpStatus): String = value.name

    @TypeConverter
    fun toRsvpStatus(value: String?): RsvpStatus =
        value?.let { try { RsvpStatus.valueOf(it) } catch (_: Exception) { RsvpStatus.PENDING } }
            ?: RsvpStatus.PENDING

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        return value?.entries?.joinToString(";") { "${it.key}=${it.value}" } ?: ""
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        return value.split(";").mapNotNull { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }
}
