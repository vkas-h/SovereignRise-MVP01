package com.sovereign_rise.app.domain.usecase.ai

import com.sovereign_rise.app.domain.model.UsageStats
import com.sovereign_rise.app.domain.repository.UsageStatsRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for collecting current usage statistics
 */
class CollectUsageStatsUseCase(
    private val usageStatsRepository: UsageStatsRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<Unit, UsageStats>(dispatcher) {
    
    override suspend fun execute(params: Unit): UsageStats {
        if (!usageStatsRepository.hasUsageStatsPermission()) {
            throw SecurityException("Usage stats permission not granted. Please enable in Settings.")
        }
        
        // Temporarily return empty stats - new analytics system handles this differently
        return UsageStats(
            timestamp = System.currentTimeMillis(),
            totalScreenTimeMinutes = 0,
            appUsageMap = emptyMap(),
            unlockCount = 0,
            mostUsedApps = emptyList()
        )
    }
}

