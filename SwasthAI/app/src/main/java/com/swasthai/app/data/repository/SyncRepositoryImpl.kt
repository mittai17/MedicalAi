package com.swasthai.app.data.repository

import com.swasthai.app.data.local.database.dao.SyncQueueDao
import com.swasthai.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementation of SyncRepository.
 * Manages the offline sync queue and synchronization with the backend.
 */
class SyncRepositoryImpl @Inject constructor(
    private val syncQueueDao: SyncQueueDao
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
                try {
                    // TODO: Send to FastAPI backend when available
                    // For now, mark as completed (simulating successful sync)
                    syncQueueDao.markAsCompleted(item.id)
                    syncedCount++
                } catch (e: Exception) {
                    syncQueueDao.markAsFailed(item.id)
                }
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearCompletedSyncItems() {
        syncQueueDao.clearCompletedItems()
    }
}
