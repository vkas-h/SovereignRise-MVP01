package com.sovereign_rise.app.data.local.dao

import androidx.room.*
import com.sovereign_rise.app.data.local.entity.TaskEntity

/**
 * DAO for Task operations.
 */
@Dao
interface TaskDao {
    
    // Insert/Update operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Transaction
    suspend fun upsertTask(task: TaskEntity) {
        insertTask(task)
    }
    
    // Query operations
    @Query("""
        SELECT * FROM tasks 
        WHERE userId = :userId 
        ORDER BY 
            CASE 
                WHEN status = 'PENDING' THEN 0 
                WHEN status = 'COMPLETED' THEN 1 
                ELSE 2 
            END, 
            createdAt ASC
    """)
    suspend fun getTasksByUserId(userId: String): List<TaskEntity>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?
    
    @Query("SELECT * FROM tasks WHERE userId = :userId AND status = :status")
    suspend fun getTasksByStatus(userId: String, status: String): List<TaskEntity>
    
    @Query("SELECT * FROM tasks WHERE syncStatus = :syncStatus")
    suspend fun getTasksBySyncStatus(syncStatus: String): List<TaskEntity>
    
    @Query("SELECT * FROM tasks WHERE userId = :userId AND createdAt >= :startTime")
    suspend fun getTasksCreatedAfter(userId: String, startTime: Long): List<TaskEntity>
    
    // Sync status updates
    @Query("UPDATE tasks SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :taskId")
    suspend fun updateSyncStatus(taskId: String, status: String, timestamp: Long)
    
    @Query("""
        UPDATE tasks 
        SET syncStatus = :status, lastSyncedAt = :timestamp, serverUpdatedAt = :serverTimestamp 
        WHERE id = :taskId
    """)
    suspend fun markAsSynced(taskId: String, status: String, timestamp: Long, serverTimestamp: Long)
    
    // Delete operations
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)
    
    @Query("DELETE FROM tasks WHERE userId = :userId AND createdAt < :cutoffTime")
    suspend fun deleteOldTasks(userId: String, cutoffTime: Long)
    
    // Aggregate queries
    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND status = 'PENDING'")
    suspend fun getPendingTaskCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM tasks WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncCount(): Int
}

