package com.bettermingle.app.data.model

data class EventPhoto(
    val id: String = "",
    val eventId: String = "",
    val uploadedBy: String = "",
    val url: String = "",
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
