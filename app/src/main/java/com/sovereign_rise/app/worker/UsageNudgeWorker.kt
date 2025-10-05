package com.sovereign_rise.app.worker

import android.content.Context
import androidx.work.*
import com.sovereign_rise.app.di.AppModule
import com.sovereign_rise.app.util.Constants
import com.sovereign_rise.app.util.NotificationHelper
import java.util.concurrent.TimeUnit

/**
 * Worker for usage-based nudges
 */
class UsageNudgeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val usageStatsRepository = AppModule.provideUsageStatsRepository(applicationContext)
            val checkNudgeTriggerUseCase = AppModule.provideCheckUsageNudgeTriggerUseCase(applicationContext)
            
            if (!usageStatsRepository.hasUsageStatsPermission()) {
                return Result.failure()
            }
            
            // TODO: Get actual user ID from auth
            val userId = "current_user"
            
            val result = checkNudgeTriggerUseCase.invoke(
                com.sovereign_rise.app.domain.usecase.ai.CheckUsageNudgeTriggerUseCase.Params(userId)
            )
            
            val shouldTrigger = result.getOrNull() ?: false
            
            if (shouldTrigger) {
                val distractingAppData = usageStatsRepository.getDistractingAppUsage(hours = 1)
                
                if (distractingAppData != null) {
                    val (appName, minutes) = distractingAppData
                    NotificationHelper.showUsageNudge(applicationContext, appName, minutes)
                    usageStatsRepository.recordNudgeSent()
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    companion object {
        fun enqueueWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<UsageNudgeWorker>(
                30, // Every 30 minutes
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(Constants.WORK_TAG_SMART_REMINDERS)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.WORK_TAG_SMART_REMINDERS,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

