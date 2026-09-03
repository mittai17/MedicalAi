package com.swasthai.app.data.repository

import com.swasthai.app.data.local.database.dao.SyncQueueDao
import com.swasthai.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementation of SyncRepository.
 *
 * syncPendingData asks [SyncUploader] to push each queued item to the real
 * backend. Uploaded items are marked complete (and later purged); failures
 * stay in the queue for the next retry.
 */
class SyncRepositoryImpl @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val syncUploader: SyncUploader
) : SyncRepository {

    override fun getPendingSyncCount(): Flow<Int> {
        return syncQueueDao.getPendingCount()
    }

    override suspend fun syncPendingData(): Result<Int> {
        return try {
            val pendingItems = syncQueueDao.getPendingItems()
            val retryableItems = syncQueueDao.getRetryableItems()
            val allItems = pendingItems + retryableItems

            var syncedCount = 0

            for (item in allItems) {
                if (syncUploader.upload(item).isSuccess) {
                    syncQueueDao.markAsCompleted(item.id)
                    syncedCount++
                } else {
                    syncQueueDao.markAsFailed(item.id)
                }
            }

            syncQueueDao.clearCompletedItems()

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearCompletedSyncItems() {
        syncQueueDao.clearCompletedItems()
    }
}