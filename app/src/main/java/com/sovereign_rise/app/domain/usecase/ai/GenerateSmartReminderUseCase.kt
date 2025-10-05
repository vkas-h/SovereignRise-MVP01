package com.sovereign_rise.app.domain.usecase.ai

import com.sovereign_rise.app.domain.model.SmartReminder
import com.sovereign_rise.app.domain.repository.SmartReminderRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for generating smart reminder suggestions
 */
class GenerateSmartReminderUseCase(
    private val smartReminderRepository: SmartReminderRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<GenerateSmartReminderUseCase.Params, SmartReminder?>(dispatcher) {
    
    data class Params(val taskId: String)
    
    override suspend fun execute(params: Params): SmartReminder? {
        require(params.taskId.isNotBlank()) { "Task ID cannot be blank" }
        return smartReminderRepository.generateSmartReminder(params.taskId)
    }
}

