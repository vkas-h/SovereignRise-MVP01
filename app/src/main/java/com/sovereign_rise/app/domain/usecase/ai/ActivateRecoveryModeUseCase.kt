package com.sovereign_rise.app.domain.usecase.ai

import com.sovereign_rise.app.domain.model.RecoveryMode
import com.sovereign_rise.app.domain.repository.BurnoutRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for activating recovery mode
 */
class ActivateRecoveryModeUseCase(
    private val burnoutRepository: BurnoutRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<ActivateRecoveryModeUseCase.Params, RecoveryMode>(dispatcher) {
    
    data class Params(val userId: String)
    
    override suspend fun execute(params: Params): RecoveryMode {
        require(params.userId.isNotBlank()) { "User ID cannot be blank" }
        return burnoutRepository.activateRecoveryMode(params.userId)
    }
}

