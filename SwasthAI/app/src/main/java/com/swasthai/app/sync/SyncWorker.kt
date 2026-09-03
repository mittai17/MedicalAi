package com.swasthai.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.swasthai.app.data.local.database.dao.SyncQueueDao
import com.swasthai.app.data.repository.SyncUploader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background SyncWorker — runs when network is available.
 *
 * Processes the SyncQueue: each pending item is turned into a real backend
 * upload by [SyncUploader]. Successful items are marked complete; failures
 * are recorded and retried (exponential back-off).
 * Tagged as "swasthai_sync" so it can be cancelled/checked by name.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncQueueDao: SyncQueueDao,
    private val syncUploader: SyncUploader
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "swasthai_sync"
        const val WORK_NAME = "SwasthAI_Periodic_Sync"

        /**
         * Enqueue a periodic sync every 15 minutes when on unmetered network.
         */
        fun enqueuePeriodicSync(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Trigger an immediate one-time sync.
         */
        fun enqueueImmediateSync(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            workManager.enqueueUniqueWork(
                "SwasthAI_Immediate_Sync",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val pendingItems = syncQueueDao.getPendingItems()

            if (pendingItems.isEmpty()) {
                return Result.success()
            }

            var allSuccess = true
            for (item in pendingItems) {
                if (syncUploader.upload(item).isSuccess) {
                    syncQueueDao.markAsCompleted(item.id)
                } else {
                    syncQueueDao.markAsFailed(item.id)
                    allSuccess = false
                }
            }

            // Purge completed items
            syncQueueDao.clearCompletedItems()

            if (allSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}