package com.bettermingle.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bettermingle.app.data.model.Event
import com.bettermingle.app.data.model.Expense
import com.bettermingle.app.data.model.Message
import com.bettermingle.app.data.model.Participant
import com.bettermingle.app.data.model.Poll
import com.bettermingle.app.data.model.PollOption
import com.bettermingle.app.data.dao.EventDao
import com.bettermingle.app.data.dao.ExpenseDao
import com.bettermingle.app.data.dao.MessageDao
import com.bettermingle.app.data.dao.ParticipantDao
import com.bettermingle.app.data.dao.PollDao

@Database(
    entities = [
        Event::class,
        Participant::class,
        Poll::class,
        PollOption::class,
        Expense::class,
        Message::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun participantDao(): ParticipantDao
    abstract fun pollDao(): PollDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bettermingle_database"
                )
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
