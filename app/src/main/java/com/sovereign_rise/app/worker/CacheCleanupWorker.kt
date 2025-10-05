package com.sovereign_rise.app.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.sovereign_rise.app.data.local.UsageStatsDatabase
import com.sovereign_rise.app.util.Constants
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for cleaning up old cached data.
 */
class CacheCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "CacheCleanupWorker"
        
        /**
         * Enqueues periodic cache cleanup work.
         */
        fun enqueueWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(false)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val cleanupWork = PeriodicWorkRequestBuilder<CacheCleanupWorker>(
                24, // 24 hours
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(Constants.WORK_TAG_CACHE_CLEANUP)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.WORK_TAG_CACHE_CLEANUP,
                ExistingPeriodicWorkPolicy.REPLACE,
                cleanupWork
            )
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting cache cleanup")
            
            val database = UsageStatsDatabase.getInstance(applicationContext)
            val taskDao = database.taskDao()
            val habitDao = database.habitDao()
            val userDao = database.userDao()
            val syncQueueDao = database.syncQueueDao()
            
            // Calculate cutoff timestamp (7 days ago)
            val cutoffTime = System.currentTimeMillis() - (Constants.OFFLINE_CACHE_DURATION_DAYS * 24 * 60 * 60 * 1000L)
            
            // Clean up old data
            // Note: We can't get userId easily here, so we'll skip user-specific cleanup
            // Only clean up expired caches and old failed sync actions
            userDao.deleteExpiredCache(cutoffTime)
            syncQueueDao.deleteOldFailedActions(cutoffTime)
            
            Log.d(TAG, "Cache cleanup completed")
            
            // Check database size
            val dbPath = applicationContext.getDatabasePath("sovereign_rise_database")
            if (dbPath.exists()) {
                val dbSizeMB = dbPath.length() / (1024 * 1024)
                Log.d(TAG, "Database size: $dbSizeMB MB")
                
                if (dbSizeMB > Constants.MAX_CACHE_SIZE_MB) {
                    Log.w(TAG, "Database size exceeds limit (${Constants.MAX_CACHE_SIZE_MB} MB)")
                    // Could implement more aggressive cleanup here if needed
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cache cleanup", e)
            Result.failure()
        }
    }
}

