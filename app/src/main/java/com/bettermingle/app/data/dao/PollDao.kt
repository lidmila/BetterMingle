package com.bettermingle.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bettermingle.app.data.model.Poll
import com.bettermingle.app.data.model.PollOption
import kotlinx.coroutines.flow.Flow

@Dao
interface PollDao {
    @Query("SELECT * FROM polls WHERE eventId = :eventId ORDER BY createdAt DESC")
    fun getPollsByEvent(eventId: String): Flow<List<Poll>>

    @Query("SELECT * FROM polls WHERE id = :pollId")
    suspend fun getPollById(pollId: String): Poll?

    @Query("SELECT * FROM poll_options WHERE pollId = :pollId ORDER BY sortOrder ASC")
    fun getOptionsByPoll(pollId: String): Flow<List<PollOption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoll(poll: Poll)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: PollOption)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<PollOption>)

    @Update
    suspend fun updatePoll(poll: Poll)

    @Query("DELETE FROM polls WHERE id = :pollId")
    suspend fun deletePoll(pollId: String)

    @Query("SELECT COUNT(*) FROM polls WHERE eventId = :eventId AND isClosed = 0")
    fun getActivePollCount(eventId: String): Flow<Int>
}
