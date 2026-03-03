package com.bettermingle.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bettermingle.app.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE eventId = :eventId ORDER BY createdAt ASC")
    fun getMessagesByEvent(eventId: String): Flow<List<Message>>

    @Query("SELECT COUNT(*) FROM messages WHERE eventId = :eventId")
    fun getMessageCount(eventId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE eventId = :eventId")
    suspend fun deleteAllByEvent(eventId: String)
}
