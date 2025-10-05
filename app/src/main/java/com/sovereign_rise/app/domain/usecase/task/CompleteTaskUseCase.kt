package com.sovereign_rise.app.domain.usecase.task

import com.sovereign_rise.app.domain.model.Task
import com.sovereign_rise.app.domain.repository.TaskRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for completing tasks.
 */
class CompleteTaskUseCase(
    private val taskRepository: TaskRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<CompleteTaskUseCase.Params, Task>(dispatcher) {
    
    override suspend fun execute(params: Params): Task {
        // Validation
        if (params.taskId.isBlank()) {
            throw IllegalArgumentException("Task ID cannot be blank")
        }
        
        // Call repository to complete task
        return taskRepository.completeTask(params.taskId)
    }
    
    data class Params(
        val taskId: String
    )
}

