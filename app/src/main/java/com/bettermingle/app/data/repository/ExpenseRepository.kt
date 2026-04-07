package com.bettermingle.app.data.repository

import android.content.Context
import com.bettermingle.app.data.database.AppDatabase
import com.bettermingle.app.data.model.Expense
import com.bettermingle.app.data.model.ExpenseSplit
import com.bettermingle.app.utils.DebtCalculator
import com.bettermingle.app.utils.Debt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.bettermingle.app.utils.safeDocuments

class ExpenseRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val expenseDao = db.expenseDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getExpensesByEvent(eventId: String): Flow<List<Expense>> =
        expenseDao.getExpensesByEvent(eventId)

    fun getTotalExpenses(eventId: String): Flow<Double?> =
        expenseDao.getTotalExpenses(eventId)

    suspend fun addExpense(
        eventId: String,
        description: String,
        amount: Double,
        currency: String = "CZK",
        category: String = "",
        splitBetween: List<String> = emptyList()
    ): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
        val expenseId = UUID.randomUUID().toString()

        val expense = Expense(
            id = expenseId,
            eventId = eventId,
            paidBy = userId,
            description = description,
            amount = amount,
            currency = currency,
            category = category
        )

        expenseDao.insertExpense(expense)

        // Create splits
        if (splitBetween.isNotEmpty()) {
            val splitAmount = DebtCalculator.equalSplit(amount, splitBetween.size)
            val splits = splitBetween.map { participantId ->
                ExpenseSplit(
                    id = UUID.randomUUID().toString(),
                    expenseId = expenseId,
                    userId = participantId,
                    amount = splitAmount
                )
            }
            syncExpenseToCloud(eventId, expense, splits)
        } else {
            syncExpenseToCloud(eventId, expense, emptyList())
        }

        return expenseId
    }

    suspend fun deleteExpense(eventId: String, expenseId: String) {
        expenseDao.deleteExpense(expenseId)
        try {
            firestore.collection("events").document(eventId)
                .collection("expenses").document(expenseId)
                .delete().await()
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to delete expense $expenseId from cloud", e)
        }
    }

    suspend fun calculateDebts(eventId: String): List<Debt> {
        // Fetch all expenses and splits from Firestore
        try {
            val expenseDocs = firestore.collection("events").document(eventId)
                .collection("expenses").get().await()

            val expenses = mutableMapOf<String, Double>() // userId -> total paid
            val shares = mutableMapOf<String, Double>()   // userId -> total owed

            for (doc in expenseDocs.safeDocuments) {
                val data = doc.data ?: continue
                val paidBy = data["paidBy"] as? String ?: continue
                val amount = (data["amount"] as? Number)?.toDouble() ?: continue

                expenses[paidBy] = (expenses[paidBy] ?: 0.0) + amount

                // Get splits for this expense
                val splitDocs = firestore.collection("events").document(eventId)
                    .collection("expenses").document(doc.id)
                    .collection("splits").get().await()

                for (splitDoc in splitDocs.safeDocuments) {
                    val splitData = splitDoc.data ?: continue
                    val splitUserId = splitData["userId"] as? String ?: continue
                    val splitAmount = (splitData["amount"] as? Number)?.toDouble() ?: continue
                    shares[splitUserId] = (shares[splitUserId] ?: 0.0) + splitAmount
                }
            }

            return DebtCalculator.calculateMinimumTransactions(expenses, shares)
        } catch (e: Exception) {
            Log.w("ExpenseRepository", "Failed to calculate debts for $eventId", e)
            return emptyList()
        }
    }

    private suspend fun syncExpenseToCloud(eventId: String, expense: Expense, splits: List<ExpenseSplit>) {
        try {
            val expenseData = mapOf(
                "paidBy" to expense.paidBy,
                "description" to expense.description,
                "amount" to expense.amount,
                "currency" to expense.currency,
                "category" to expense.category,
                "receiptUrl" to expense.receiptUrl,
                "createdAt" to expense.createdAt
            )
            val expenseRef = firestore.collection("events").document(eventId)
                .collection("expenses").document(expense.id)
            expenseRef.set(expenseData, SetOptions.merge()).await()

            splits.forEach { split ->
                val splitData = mapOf(
                    "userId" to split.userId,
                    "amount" to split.amount,
                    "isSettled" to split.isSettled
                )
                expenseRef.collection("splits").document(split.id)
                    .set(splitData, SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to sync expense ${expense.id} to cloud", e)
        }
    }

    suspend fun syncFromCloud(eventId: String) {
        try {
            val docs = firestore.collection("events").document(eventId)
                .collection("expenses").get().await()

            for (doc in docs.safeDocuments) {
                val data = doc.data ?: continue
                val expense = Expense(
                    id = doc.id,
                    eventId = eventId,
                    paidBy = data["paidBy"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    currency = data["currency"] as? String ?: "CZK",
                    category = data["category"] as? String ?: "",
                    receiptUrl = data["receiptUrl"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                expenseDao.insertExpense(expense)
            }
        } catch (e: Exception) {
            Log.w("ExpenseRepository", "Failed to sync expenses from cloud for $eventId", e)
        }
    }
}
