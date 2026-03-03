package com.bettermingle.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String = "",
    val eventId: String = "",
    val paidBy: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val currency: String = "CZK",
    val category: String = "",
    val receiptUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ExpenseSplit(
    val id: String = "",
    val expenseId: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val isSettled: Boolean = false
)
