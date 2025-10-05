package com.sovereign_rise.app.domain.usecase.ai

import com.sovereign_rise.app.domain.model.BurnoutAlert
import com.sovereign_rise.app.domain.repository.BurnoutRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for detecting burnout
 */
class DetectBurnoutUseCase(
    private val burnoutRepository: BurnoutRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<DetectBurnoutUseCase.Params, BurnoutAlert?>(dispatcher) {
    
    data class Params(val userId: String)
    
    override suspend fun execute(params: Params): BurnoutAlert? {
        require(params.userId.isNotBlank()) { "User ID cannot be blank" }
        return burnoutRepository.detectBurnout(params.userId)
    }
}

