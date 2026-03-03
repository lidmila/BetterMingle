package com.bettermingle.app.data.model

data class PackingItem(
    val id: String = "",
    val eventId: String = "",
    val name: String = "",
    val isChecked: Boolean = false,
    val userId: String? = null,
    val addedBy: String = ""
)
