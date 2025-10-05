package com.sovereign_rise.app.workers

import android.content.Context
import androidx.work.*
import com.sovereign_rise.app.di.AppModule
import com.sovereign_rise.app.util.ErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker to periodically sync usage stats to backend
 */
class UsageSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if tracking is enabled
            val trackingPrefs = com.sovereign_rise.app.data.local.UsageTrackingPreferences(applicationContext)
            val isEnabled = trackingPrefs.getTrackingEnabled()
            
            if (!isEnabled) {
                // Tracking is disabled, cancel the worker
                cancel(applicationContext)
                return@withContext Result.success()
            }
            
            // Get repository from DI
            val repository = AppModule.provideUsageStatsRepository(applicationContext)

            // Sync today's usage data
            val syncResult = repository.syncTodayUsageData()
            
            if (syncResult.isSuccess) {
                Result.success()
            } else {
                ErrorLogger.logException(
                    syncResult.exceptionOrNull() ?: Exception("Unknown error"),
                    "UsageSyncWorker.doWork"
                )
                Result.retry()
            }
        } catch (e: Exception) {
            ErrorLogger.logException(e, "UsageSyncWorker.doWork")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "usage_sync_work"
        private const val IMMEDIATE_WORK_NAME = "usage_sync_immediate"

        /**
         * Schedule periodic usage sync
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<UsageSyncWorker>(
                6, TimeUnit.HOURS // Sync every 6 hours
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }

        /**
         * Trigger immediate one-time sync
         */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<UsageSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    IMMEDIATE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
        }

        /**
         * Cancel scheduled work
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

