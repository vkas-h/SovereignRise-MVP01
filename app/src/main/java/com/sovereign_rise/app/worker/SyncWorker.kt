package com.sovereign_rise.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.sovereign_rise.app.MainActivity
import com.sovereign_rise.app.data.sync.SyncManager
import com.sovereign_rise.app.di.AppModule
import com.sovereign_rise.app.util.Constants
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for periodic sync of pending actions.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SyncWorker"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sync_channel"
        
        /**
         * Enqueues periodic sync work.
         */
        fun enqueueWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
                Constants.SYNC_CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(Constants.WORK_TAG_SYNC_ENGINE)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.WORK_TAG_SYNC_ENGINE,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWork
            )
        }
        
        /**
         * Enqueues immediate one-time sync.
         */
        fun enqueueSyncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag(Constants.WORK_TAG_SYNC_ENGINE)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
            WorkManager.getInstance(context).enqueue(syncWork)
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            // Get SyncManager from AppModule
            val syncManager = AppModule.provideSyncManager(applicationContext)
            
            // Check if there are pending actions
            val pendingCount = syncManager.getPendingSyncCount()
            if (pendingCount == 0) {
                return Result.success()
            }
            
            // Check network connectivity
            if (!syncManager.isOnline()) {
                return Result.retry()
            }
            
            // Perform sync
            val result = syncManager.syncPendingActions()
            
            return if (result.success || result.failedCount == 0) {
                Result.success()
            } else if (result.failedCount > 0 && runAttemptCount < Constants.SYNC_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                // Max retries exceeded - show notification
                showSyncFailedNotification()
                Result.failure()
            }
        } catch (e: Exception) {
            if (runAttemptCount < Constants.SYNC_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    private fun showSyncFailedNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel (Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sync Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for sync status"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create retry intent
        val retryIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val retryPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            retryIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Sync Failed")
            .setContentText("Some changes couldn't be synced. Check your connection.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(retryPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_rotate,
                "Retry Now",
                retryPendingIntent
            )
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

