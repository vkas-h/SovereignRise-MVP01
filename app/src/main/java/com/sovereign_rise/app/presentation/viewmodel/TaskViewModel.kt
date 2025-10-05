package com.sovereign_rise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sovereign_rise.app.data.sync.SyncManager
import com.sovereign_rise.app.domain.model.Affirmation
import com.sovereign_rise.app.domain.model.AffirmationContext
import com.sovereign_rise.app.domain.model.SyncStatus
import com.sovereign_rise.app.domain.model.Task
import com.sovereign_rise.app.domain.model.TaskDifficulty
import com.sovereign_rise.app.domain.usecase.ai.GenerateAffirmationUseCase
import com.sovereign_rise.app.domain.usecase.task.*
import com.sovereign_rise.app.presentation.components.SyncState
import com.sovereign_rise.app.util.ConnectivityObserver
import com.sovereign_rise.app.util.ConnectivityStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for tasks screen.
 * Manages task list UI state and coordinates task-related use cases.
 */
class TaskViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val triggerDailyResetUseCase: TriggerDailyResetUseCase,
    private val generateAffirmationUseCase: GenerateAffirmationUseCase,
    private val syncManager: SyncManager,
    private val connectivityObserver: ConnectivityObserver,
    private val taskRepository: com.sovereign_rise.app.data.repository.TaskRepositoryImpl
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()
    
    // Selected task for edit screen
    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()
    
    // Completion result for display
    private val _completionResult = MutableStateFlow<Task?>(null)
    val completionResult: StateFlow<Task?> = _completionResult.asStateFlow()
    
    // Affirmation for motivational messages
    private val _affirmation = MutableStateFlow<Affirmation?>(null)
    val affirmation: StateFlow<Affirmation?> = _affirmation.asStateFlow()
    
    // Sync state for offline mode
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Synced)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    init {
        // Monitor sync status
        monitorSyncStatus()
        // Trigger daily reset before loading tasks
        viewModelScope.launch {
            try {
                triggerDailyResetUseCase(Unit)
            } catch (e: Exception) {
                // Log error but continue to load tasks
                e.printStackTrace()
            }
            // Load tasks after reset
            loadTasks()
        }
    }
    
    /**
     * Loads tasks from the repository via use case.
     */
    fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = TaskUiState.Loading
            
            try {
                // Fetch tasks with sync status
                val tasksWithStatus = taskRepository.getTasksWithSyncStatus()
                val taskViewModels = tasksWithStatus.map { (task, syncStatus) ->
                    TaskWithSyncStatus(task, syncStatus)
                }
                
                _uiState.value = if (taskViewModels.isEmpty()) {
                    TaskUiState.Empty
                } else {
                    TaskUiState.Success(taskViewModels)
                }
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to load tasks")
            }
        }
    }
    
    /**
     * Triggers the daily reset.
     */
    fun triggerDailyReset() {
        viewModelScope.launch {
            try {
                val result = triggerDailyResetUseCase(Unit)
                result.fold(
                    onSuccess = { 
                        loadTasks()
                    },
                    onFailure = { error ->
                        _uiState.value = TaskUiState.Error(error.message ?: "Failed to trigger daily reset")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to trigger daily reset")
            }
        }
    }
    
    /**
     * Creates a new task.
     */
    fun createTask(
        title: String,
        description: String?,
        difficulty: TaskDifficulty,
        reminderTime: Long?
    ) {
        viewModelScope.launch {
            _uiState.value = TaskUiState.Loading
            
            val params = CreateTaskUseCase.Params(
                title = title,
                description = description,
                difficulty = difficulty,
                reminderTime = reminderTime
            )
            
            val result = createTaskUseCase(params)
            
            result.fold(
                onSuccess = { 
                    _uiState.value = TaskUiState.TaskSaved
                    // Don't load tasks here - will load when returning to task list
                },
                onFailure = { error ->
                    _uiState.value = TaskUiState.Error(error.message ?: "Failed to create task")
                }
            )
        }
    }
    
    /**
     * Updates an existing task.
     */
    fun updateTask(
        taskId: String,
        title: String?,
        description: String?,
        difficulty: TaskDifficulty?,
        reminderTime: Long?
    ) {
        viewModelScope.launch {
            _uiState.value = TaskUiState.Loading
            
            val params = UpdateTaskUseCase.Params(
                taskId = taskId,
                title = title,
                description = description,
                difficulty = difficulty,
                reminderTime = reminderTime
            )
            
            val result = updateTaskUseCase(params)
            
            result.fold(
                onSuccess = { 
                    _uiState.value = TaskUiState.TaskSaved
                    // Don't load tasks here - will load when returning to task list
                },
                onFailure = { error ->
                    _uiState.value = TaskUiState.Error(error.message ?: "Failed to update task")
                }
            )
        }
    }
    
    /**
     * Deletes a task.
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val params = DeleteTaskUseCase.Params(taskId = taskId)
            
            val result = deleteTaskUseCase(params)
            
            result.fold(
                onSuccess = { 
                    _uiState.value = TaskUiState.TaskDeleted(taskId)
                    loadTasks()
                },
                onFailure = { error ->
                    _uiState.value = TaskUiState.Error(error.message ?: "Failed to delete task")
                }
            )
        }
    }
    
    /**
     * Completes a task and awards XP/Aether.
     */
    fun completeTask(taskId: String) {
        viewModelScope.launch {
            val params = CompleteTaskUseCase.Params(taskId = taskId)
            
            val result = completeTaskUseCase(params)
            
            result.fold(
                onSuccess = { completedTask ->
                    _completionResult.value = completedTask
                    loadTasks()
                    
                    // Generate affirmation after task completion
                    generateAffirmation(
                        context = AffirmationContext.TASK_COMPLETED,
                        variables = emptyMap()
                    )
                },
                onFailure = { error ->
                    _uiState.value = TaskUiState.Error(error.message ?: "Failed to complete task")
                }
            )
        }
    }
    
    /**
     * Generates an affirmation message.
     */
    private fun generateAffirmation(context: AffirmationContext, variables: Map<String, String>) {
        viewModelScope.launch {
            try {
                val params = GenerateAffirmationUseCase.Params(
                    userId = "current_user", // TODO: Replace with actual user ID
                    context = context,
                    variables = variables
                )
                val result = generateAffirmationUseCase(params)
                result.fold(
                    onSuccess = { affirmation ->
                        _affirmation.value = affirmation
                    },
                    onFailure = { 
                        // Silently fail - affirmations are optional
                    }
                )
            } catch (e: Exception) {
                // Silently fail - affirmations are optional
            }
        }
    }
    
    /**
     * Selects a task for editing (by ID).
     */
    fun selectTask(taskId: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is TaskUiState.Success) {
                    val taskWithStatus = currentState.tasks.find { it.task.id == taskId }
                    _selectedTask.value = taskWithStatus?.task
                }
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to select task")
            }
        }
    }
    
    /**
     * Selects a task for editing (directly with Task object).
     */
    fun selectTask(task: Task) {
        _selectedTask.value = task
    }
    
    /**
     * Clears the selected task.
     */
    fun clearSelectedTask() {
        _selectedTask.value = null
    }
    
    /**
     * Clears the completion result.
     */
    fun clearCompletionResult() {
        _completionResult.value = null
    }
    
    /**
     * Clears the level-up result.
     */
    fun clearLevelUpResult() {
    }
    
    /**
     * Clears the affirmation.
     */
    fun clearAffirmation() {
        _affirmation.value = null
    }
    
    /**
     * Consumes the TaskSaved event and resets to loading state.
     */
    fun consumeTaskSavedEvent() {
        // Reset to Loading state - tasks will be loaded by TaskListScreen
        _uiState.value = TaskUiState.Loading
    }
    
    /**
     * Resets UI state to idle.
     */
    fun resetState() {
        loadTasks()
    }
    
    /**
     * Refreshes the task list.
     */
    fun refresh() {
        loadTasks()
    }
    
    /**
     * Monitors sync status and connectivity.
     * Automatically triggers sync when connectivity is available.
     */
    private fun monitorSyncStatus() {
        viewModelScope.launch {
            // Monitor connectivity
            connectivityObserver.observe().collect { status ->
                when (status) {
                    ConnectivityStatus.AVAILABLE -> {
                        val pendingCount = syncManager.getPendingSyncCount()
                        if (pendingCount > 0) {
                            // Automatically trigger sync when connected
                            _syncState.value = SyncState.Syncing(0, pendingCount)
                            try {
                                val result = syncManager.syncPendingActions()
                                _syncState.value = if (result.success) {
                                    SyncState.Synced
                                } else {
                                    SyncState.Failed(result.failedCount, result.errors)
                                }
                                // Reload tasks after sync
                                if (result.success) {
                                    loadTasks()
                                }
                            } catch (e: Exception) {
                                _syncState.value = SyncState.Failed(pendingCount, listOf(e.message ?: "Unknown error"))
                            }
                        } else {
                            _syncState.value = SyncState.Synced
                        }
                    }
                    ConnectivityStatus.UNAVAILABLE -> {
                        val pendingCount = syncManager.getPendingSyncCount()
                        _syncState.value = if (pendingCount > 0) {
                            SyncState.Offline
                        } else {
                            SyncState.Synced
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * Manually triggers sync.
     */
    fun syncNow() {
        viewModelScope.launch {
            val pendingCount = syncManager.getPendingSyncCount()
            if (pendingCount > 0) {
                _syncState.value = SyncState.Syncing(0, pendingCount)
                try {
                    val result = syncManager.syncPendingActions()
                    _syncState.value = if (result.success) {
                        SyncState.Synced
                    } else {
                        SyncState.Failed(result.failedCount, result.errors)
                    }
                } catch (e: Exception) {
                    _syncState.value = SyncState.Failed(pendingCount, listOf(e.message ?: "Unknown error"))
                }
            }
        }
    }
    
    /**
     * Retries failed sync actions.
     */
    fun retryFailedSync() {
        syncNow()
    }
}

/**
 * Data class combining Task with its sync status for UI display.
 */
data class TaskWithSyncStatus(
    val task: Task,
    val syncStatus: com.sovereign_rise.app.domain.model.SyncStatus
)

/**
 * Sealed class representing task list UI states.
 */
sealed class TaskUiState {
    data object Loading : TaskUiState()
    data object Empty : TaskUiState()
    data class Success(val tasks: List<TaskWithSyncStatus>) : TaskUiState()
    data class Error(val message: String) : TaskUiState()
    data class TaskDeleted(val taskId: String) : TaskUiState()
    data object TaskSaved : TaskUiState()
}
