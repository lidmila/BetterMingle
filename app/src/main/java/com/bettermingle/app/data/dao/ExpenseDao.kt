package com.bettermingle.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bettermingle.app.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE eventId = :eventId ORDER BY createdAt DESC")
    fun getExpensesByEvent(eventId: String): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE eventId = :eventId")
    fun getTotalExpenses(eventId: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    @Query("DELETE FROM expenses WHERE eventId = :eventId")
    suspend fun deleteAllByEvent(eventId: String)
}
