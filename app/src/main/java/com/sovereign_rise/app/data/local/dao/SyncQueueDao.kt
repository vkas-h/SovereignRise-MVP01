package com.sovereign_rise.app.data.local.dao

import androidx.room.*
import com.sovereign_rise.app.data.local.entity.SyncQueueEntity

/**
 * DAO for sync queue management.
 */
@Dao
interface SyncQueueDao {
    
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueAction(action: SyncQueueEntity)
    
    @Insert
    suspend fun enqueueAll(actions: List<SyncQueueEntity>)
    
    // Query operations
    @Query("""
        SELECT * FROM sync_queue 
        WHERE status = 'PENDING' 
        ORDER BY priority ASC, createdAt ASC 
        LIMIT :limit
    """)
    suspend fun getPendingActions(limit: Int): List<SyncQueueEntity>
    
    @Query("SELECT * FROM sync_queue WHERE id = :actionId")
    suspend fun getActionById(actionId: String): SyncQueueEntity?
    
    @Query("""
        SELECT * FROM sync_queue 
        WHERE entityType = :entityType AND entityId = :entityId AND status = 'PENDING'
    """)
    suspend fun getPendingActionsForEntity(entityType: String, entityId: String): List<SyncQueueEntity>
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'FAILED'")
    suspend fun getFailedCount(): Int
    
    // Status updates
    @Query("UPDATE sync_queue SET status = :status, lastAttemptAt = :timestamp WHERE id = :actionId")
    suspend fun updateActionStatus(actionId: String, status: String, timestamp: Long)
    
    @Query("""
        UPDATE sync_queue 
        SET status = :status, retryCount = :retryCount, lastAttemptAt = :timestamp, errorMessage = :error 
        WHERE id = :actionId
    """)
    suspend fun markActionFailed(
        actionId: String, 
        status: String, 
        retryCount: Int, 
        timestamp: Long, 
        error: String
    )
    
    @Query("UPDATE sync_queue SET status = 'SYNCING' WHERE id = :actionId")
    suspend fun markActionSyncing(actionId: String)
    
    // Delete operations
    @Query("DELETE FROM sync_queue WHERE id = :actionId")
    suspend fun deleteAction(actionId: String)
    
    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun deleteAllSynced()
    
    @Query("DELETE FROM sync_queue WHERE createdAt < :cutoffTime AND status = 'FAILED'")
    suspend fun deleteOldFailedActions(cutoffTime: Long)
    
    // Queue management
    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getQueueSize(): Int
    
    @Query("""
        DELETE FROM sync_queue 
        WHERE id IN (
            SELECT id FROM sync_queue 
            ORDER BY priority DESC, createdAt DESC 
            LIMIT :count
        )
    """)
    suspend fun trimQueue(count: Int)
}

