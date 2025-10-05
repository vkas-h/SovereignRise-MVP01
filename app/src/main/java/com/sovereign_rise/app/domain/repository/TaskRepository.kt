package com.sovereign_rise.app.domain.repository

import com.sovereign_rise.app.domain.model.Task

/**
 * Repository interface for task operations.
 * Defines the contract for task data operations.
 */
interface TaskRepository {
    
    /**
     * Gets all tasks for the current user.
     * @return List of tasks
     */
    suspend fun getTasks(): List<Task>
    
    /**
     * Gets a specific task by ID.
     * @param taskId The task's ID
     * @return Task object if found
     */
    suspend fun getTaskById(taskId: String): Task
    
    /**
     * Creates a new task.
     * @param task Task object to create
     * @return Created task with generated ID
     */
    suspend fun createTask(task: Task): Task
    
    /**
     * Updates an existing task.
     * @param task Task object with updated fields
     * @return Updated task object
     */
    suspend fun updateTask(task: Task): Task
    
    /**
     * Deletes a task by ID.
     * @param taskId The task's ID
     */
    suspend fun deleteTask(taskId: String)
    
    /**
     * Marks a task as completed.
     * @param taskId The task's ID
     * @return Updated task object
     */
    suspend fun completeTask(taskId: String): Task
    
    /**
     * Triggers the daily reset process.
     * Marks uncompleted tasks as failed and applies penalties.
     * @return Updated list of tasks after reset
     */
    suspend fun triggerDailyReset(): List<Task>
}
