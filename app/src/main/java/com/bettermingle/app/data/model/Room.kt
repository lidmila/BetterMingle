package com.bettermingle.app.data.model

data class EventRoom(
    val id: String = "",
    val eventId: String = "",
    val name: String = "",
    val capacity: Int = 2,
    val notes: String = "",
    val assignments: List<String> = emptyList()
)

data class RoomAssignment(
    val roomId: String = "",
    val userId: String = "",
    val displayName: String = ""
)
