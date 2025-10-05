package com.sovereign_rise.app.domain.usecase.task

import com.sovereign_rise.app.domain.model.Task
import com.sovereign_rise.app.domain.model.TaskDifficulty
import com.sovereign_rise.app.domain.model.TaskStatus
import com.sovereign_rise.app.domain.repository.TaskRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import com.sovereign_rise.app.util.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for updating existing tasks.
 */
class UpdateTaskUseCase(
    private val taskRepository: TaskRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<UpdateTaskUseCase.Params, Task>(dispatcher) {
    
    override suspend fun execute(params: Params): Task {
        // Validation
        if (params.taskId.isBlank()) {
            throw IllegalArgumentException("Task ID cannot be blank")
        }
        if (params.title != null) {
            if (params.title.length < Constants.MIN_TASK_TITLE_LENGTH) {
                throw IllegalArgumentException(Constants.ERROR_TASK_TITLE_TOO_SHORT)
            }
            if (params.title.length > Constants.MAX_TASK_TITLE_LENGTH) {
                throw IllegalArgumentException(Constants.ERROR_TASK_TITLE_TOO_LONG)
            }
        }
        if (params.description != null && params.description.length > Constants.MAX_TASK_DESCRIPTION_LENGTH) {
            throw IllegalArgumentException(Constants.ERROR_TASK_DESCRIPTION_TOO_LONG)
        }
        if (params.reminderTime != null && params.reminderTime <= System.currentTimeMillis()) {
            throw IllegalArgumentException(Constants.ERROR_REMINDER_IN_PAST)
        }
        
        // Get existing task
        val existingTask = taskRepository.getTaskById(params.taskId)
        
        // Cannot update status to COMPLETED via this use case
        if (existingTask.status == TaskStatus.COMPLETED) {
            throw IllegalArgumentException("Cannot update a completed task")
        }
        
        // Apply updates
        val updatedTitle = params.title ?: existingTask.title
        val updatedDescription = params.description ?: existingTask.description
        val updatedDifficulty = params.difficulty ?: existingTask.difficulty
        val updatedReminderTime = params.reminderTime ?: existingTask.reminderTime
        
        // Create updated task
        val updatedTask = existingTask.copy(
            title = updatedTitle,
            description = updatedDescription,
            difficulty = updatedDifficulty,
            reminderTime = updatedReminderTime
        )
        
        // Call repository to update task
        return taskRepository.updateTask(updatedTask)
    }
    
    data class Params(
        val taskId: String,
        val title: String?,
        val description: String?,
        val difficulty: TaskDifficulty?,
        val reminderTime: Long?
    )
}

