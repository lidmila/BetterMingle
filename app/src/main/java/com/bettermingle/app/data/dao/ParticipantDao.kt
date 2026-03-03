package com.bettermingle.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bettermingle.app.data.model.Participant
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantDao {
    @Query("SELECT * FROM participants WHERE eventId = :eventId")
    fun getParticipantsByEvent(eventId: String): Flow<List<Participant>>

    @Query("SELECT COUNT(*) FROM participants WHERE eventId = :eventId")
    fun getParticipantCount(eventId: String): Flow<Int>

    @Query("SELECT * FROM participants WHERE eventId = :eventId AND userId = :userId")
    suspend fun getParticipant(eventId: String, userId: String): Participant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: Participant)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<Participant>)

    @Delete
    suspend fun deleteParticipant(participant: Participant)

    @Query("DELETE FROM participants WHERE eventId = :eventId")
    suspend fun deleteAllByEvent(eventId: String)
}
