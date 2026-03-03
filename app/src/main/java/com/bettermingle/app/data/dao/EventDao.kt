package com.bettermingle.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bettermingle.app.data.model.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startDate ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    fun getEventById(eventId: String): Flow<Event?>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventByIdOnce(eventId: String): Event?

    @Query("SELECT * FROM events WHERE status != 'COMPLETED' AND status != 'CANCELLED' ORDER BY startDate ASC")
    fun getActiveEvents(): Flow<List<Event>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<Event>)

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Query("SELECT COUNT(*) FROM events WHERE status != 'COMPLETED' AND status != 'CANCELLED'")
    fun getActiveEventCount(): Flow<Int>
}
