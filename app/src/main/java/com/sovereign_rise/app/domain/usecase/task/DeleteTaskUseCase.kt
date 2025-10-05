package com.sovereign_rise.app.domain.usecase.task

import com.sovereign_rise.app.domain.repository.TaskRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for deleting tasks.
 */
class DeleteTaskUseCase(
    private val taskRepository: TaskRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<DeleteTaskUseCase.Params, Unit>(dispatcher) {
    
    override suspend fun execute(params: Params) {
        // Validation
        if (params.taskId.isBlank()) {
            throw IllegalArgumentException("Task ID cannot be blank")
        }
        
        // Call repository to delete task
        taskRepository.deleteTask(params.taskId)
    }
    
    data class Params(
        val taskId: String
    )
}

