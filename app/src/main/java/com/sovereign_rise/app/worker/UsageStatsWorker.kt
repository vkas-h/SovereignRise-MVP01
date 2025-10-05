package com.sovereign_rise.app.worker

import android.content.Context
import androidx.work.*
import com.sovereign_rise.app.di.AppModule
import com.sovereign_rise.app.util.Constants
import java.util.concurrent.TimeUnit

/**
 * Worker for periodic usage stats collection
 */
class UsageStatsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val usageStatsRepository = AppModule.provideUsageStatsRepository(applicationContext)
            
            if (!usageStatsRepository.hasUsageStatsPermission()) {
                android.util.Log.w("UsageStatsWorker", "Permission not granted, skipping")
                return Result.failure()
            }
            
            // Check if we've already synced recently (within last hour)
            val prefs = applicationContext.getSharedPreferences("usage_stats_prefs", Context.MODE_PRIVATE)
            val lastSyncTime = prefs.getLong("last_sync_time", 0)
            val currentTime = System.currentTimeMillis()
            val hoursSinceLastSync = (currentTime - lastSyncTime) / (1000 * 60 * 60)
            
            if (hoursSinceLastSync < 1) {
                android.util.Log.d("UsageStatsWorker", "Already synced ${hoursSinceLastSync.toInt()} hours ago, skipping")
                return Result.success()
            }
            
            android.util.Log.d("UsageStatsWorker", "Starting usage stats sync...")
            
            // Sync today's data
            val result = usageStatsRepository.syncTodayUsageData()
            
            if (result.isSuccess) {
                // Update last sync time
                prefs.edit().putLong("last_sync_time", currentTime).apply()
                android.util.Log.d("UsageStatsWorker", "Sync successful: ${result.getOrNull()}")
                Result.success()
            } else {
                android.util.Log.e("UsageStatsWorker", "Sync failed: ${result.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: SecurityException) {
            android.util.Log.e("UsageStatsWorker", "Security exception: ${e.message}")
            Result.failure()
        } catch (e: Exception) {
            android.util.Log.e("UsageStatsWorker", "Error: ${e.message}", e)
            Result.retry()
        }
    }
    
    companion object {
        fun enqueueWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED) // Only sync when online
                .build()
            
            // Sync every 4 hours instead of every hour to reduce duplicate syncs
            val workRequest = PeriodicWorkRequestBuilder<UsageStatsWorker>(
                4, // Changed from 1 to 4 hours
                TimeUnit.HOURS,
                15, // Flex interval
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(Constants.WORK_TAG_USAGE_STATS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.WORK_TAG_USAGE_STATS,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            android.util.Log.d("UsageStatsWorker", "Scheduled periodic usage stats sync (every 4 hours)")
        }
    }
}

