package com.bettermingle.app.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    const val UNIQUE_SYNC_WORK = "bettermingle_sync"

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    fun enqueueSync(context: Context, replace: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        val policy = if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_SYNC_WORK, policy, syncRequest)

        Log.d(TAG, "Sync enqueued (replace=$replace)")
    }

    fun enqueueSyncForEvent(context: Context, eventId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf("eventId" to eventId))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("sync_event_$eventId", ExistingWorkPolicy.REPLACE, syncRequest)
    }

    fun updateStatus(status: SyncStatus) {
        _syncStatus.value = status
        Log.d(TAG, "Sync status: $status")
    }
}

enum class SyncStatus {
    IDLE, SYNCING, SUCCESS, ERROR
}
