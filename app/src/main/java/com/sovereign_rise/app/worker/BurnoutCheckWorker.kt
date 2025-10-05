package com.sovereign_rise.app.worker

import android.content.Context
import androidx.work.*
import com.sovereign_rise.app.di.AppModule
import com.sovereign_rise.app.domain.model.BurnoutLevel
import com.sovereign_rise.app.util.Constants
import com.sovereign_rise.app.util.NotificationHelper
import java.util.concurrent.TimeUnit

/**
 * Worker for daily burnout detection
 */
class BurnoutCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val detectBurnoutUseCase = AppModule.provideDetectBurnoutUseCase(applicationContext)
            val activateRecoveryModeUseCase = AppModule.provideActivateRecoveryModeUseCase(applicationContext)
            
            // TODO: Get actual user ID from auth
            val userId = "current_user"
            
            val result = detectBurnoutUseCase.invoke(
                com.sovereign_rise.app.domain.usecase.ai.DetectBurnoutUseCase.Params(userId)
            )
            
            result.getOrNull()?.let { alert ->
                when (alert.level) {
                    BurnoutLevel.SEVERE -> {
                        // Automatically activate recovery mode
                        activateRecoveryModeUseCase.invoke(
                            com.sovereign_rise.app.domain.usecase.ai.ActivateRecoveryModeUseCase.Params(userId)
                        )
                        NotificationHelper.showBurnoutAlert(applicationContext, alert, autoRecovery = true)
                    }
                    BurnoutLevel.MODERATE -> {
                        NotificationHelper.showBurnoutAlert(applicationContext, alert, autoRecovery = false)
                    }
                    else -> { /* No action needed */ }
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
            
            // Schedule to run at midnight
            val currentTime = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTime
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            val initialDelay = calendar.timeInMillis - currentTime
            
            val workRequest = PeriodicWorkRequestBuilder<BurnoutCheckWorker>(
                Constants.BURNOUT_CHECK_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(Constants.WORK_TAG_BURNOUT_CHECK)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.WORK_TAG_BURNOUT_CHECK,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}

