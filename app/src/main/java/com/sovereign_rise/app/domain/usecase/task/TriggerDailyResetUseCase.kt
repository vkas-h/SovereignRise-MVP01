package com.sovereign_rise.app.domain.usecase.task

import com.sovereign_rise.app.domain.model.Task
import com.sovereign_rise.app.domain.repository.TaskRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for triggering the daily reset.
 */
class TriggerDailyResetUseCase(
    private val taskRepository: TaskRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<Unit, List<Task>>(dispatcher) {
    
    override suspend fun execute(params: Unit): List<Task> {
        // Call repository to trigger daily reset
        // Backend handles checking if reset is needed based on last reset timestamp
        return taskRepository.triggerDailyReset()
    }
}

