package com.sovereign_rise.app.domain.usecase.ai

import com.sovereign_rise.app.domain.repository.UsageStatsRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for checking if usage-based nudge should be triggered
 */
class CheckUsageNudgeTriggerUseCase(
    private val usageStatsRepository: UsageStatsRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<CheckUsageNudgeTriggerUseCase.Params, Boolean>(dispatcher) {
    
    data class Params(val userId: String)
    
    override suspend fun execute(params: Params): Boolean {
        require(params.userId.isNotBlank()) { "User ID cannot be blank" }
        return usageStatsRepository.shouldTriggerNudge()
    }
}

