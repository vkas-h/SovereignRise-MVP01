package com.sovereign_rise.app.domain.usecase.task

import com.sovereign_rise.app.domain.model.Task
import com.sovereign_rise.app.domain.repository.TaskRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for retrieving user's tasks.
 * Encapsulates business logic for fetching and filtering tasks.
 * 
 * @param taskRepository Repository for task operations
 * @param dispatcher Coroutine dispatcher for execution (defaults to IO)
 */
class GetTasksUseCase(
    private val taskRepository: TaskRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<Unit, List<Task>>(dispatcher) {
    
    override suspend fun execute(params: Unit): List<Task> {
        // Fetch tasks from repository
        val tasks = taskRepository.getTasks()
        
        // Business logic: sort tasks by status and created date
        return tasks.sortedWith(
            compareBy<Task> { 
                when (it.status) {
                    com.sovereign_rise.app.domain.model.TaskStatus.PENDING -> 0
                    com.sovereign_rise.app.domain.model.TaskStatus.COMPLETED -> 1
                    com.sovereign_rise.app.domain.model.TaskStatus.PARTIAL -> 2
                    com.sovereign_rise.app.domain.model.TaskStatus.FAILED -> 3
                }
            }.thenBy { 
                if (it.status == com.sovereign_rise.app.domain.model.TaskStatus.PENDING) it.createdAt else -it.createdAt 
            }
        )
    }
}
