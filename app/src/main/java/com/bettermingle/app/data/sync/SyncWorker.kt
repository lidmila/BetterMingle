package com.bettermingle.app.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bettermingle.app.data.repository.EventRepository
import com.bettermingle.app.data.repository.ExpenseRepository
import com.bettermingle.app.data.repository.PollRepository

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val eventId = inputData.getString("eventId")
        CloudSyncManager.updateStatus(SyncStatus.SYNCING)

        return try {
            val eventRepo = EventRepository(applicationContext)
            val pollRepo = PollRepository(applicationContext)
            val expenseRepo = ExpenseRepository(applicationContext)

            if (eventId != null) {
                Log.d(TAG, "Syncing event: $eventId")
                pollRepo.syncFromCloud(eventId)
                expenseRepo.syncFromCloud(eventId)
            } else {
                Log.d(TAG, "Starting full sync")
                eventRepo.syncFromCloud()
            }

            CloudSyncManager.updateStatus(SyncStatus.SUCCESS)
            Log.d(TAG, "Sync completed successfully")
            androidx.work.ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            CloudSyncManager.updateStatus(SyncStatus.ERROR)
            if (runAttemptCount < 3) {
                androidx.work.ListenableWorker.Result.retry()
            } else {
                androidx.work.ListenableWorker.Result.failure()
            }
        }
    }
}
