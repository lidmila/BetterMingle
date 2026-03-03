package com.bettermingle.app.data.model

data class ScheduleItem(
    val id: String = "",
    val eventId: String = "",
    val title: String = "",
    val description: String = "",
    val startTime: Long? = null,
    val endTime: Long? = null,
    val location: String = ""
)
