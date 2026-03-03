package com.bettermingle.app.data.model

data class EventLabel(
    val id: String = "",
    val eventId: String = "",
    val name: String = "",
    val color: String = "",
    val assignedTo: List<String> = emptyList(),
    val deadline: Long? = null,
    val isCompleted: Boolean = false
)
