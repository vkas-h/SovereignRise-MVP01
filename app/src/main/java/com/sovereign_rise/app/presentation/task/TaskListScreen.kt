package com.sovereign_rise.app.presentation.task

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sovereign_rise.app.domain.model.Task
import com.sovereign_rise.app.domain.model.TaskDifficulty
import com.sovereign_rise.app.domain.model.TaskStatus
import com.sovereign_rise.app.domain.model.TutorialDay
import com.sovereign_rise.app.presentation.components.*
import com.sovereign_rise.app.presentation.navigation.Screen
import com.sovereign_rise.app.presentation.viewmodel.TaskUiState
import com.sovereign_rise.app.presentation.viewmodel.TaskViewModel
import com.sovereign_rise.app.ui.theme.*
import com.sovereign_rise.app.util.optimizedListItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    navController: NavController,
    viewModel: TaskViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val completionResult by viewModel.completionResult.collectAsState()
    val affirmation by viewModel.affirmation.collectAsState()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Tutorial state
    var tutorialState by remember { mutableStateOf<com.sovereign_rise.app.domain.model.TutorialState?>(null) }
    var showTutorialLimitDialog by remember { mutableStateOf(false) }
    
    // Load tutorial state on screen launch
    LaunchedEffect(Unit) {
        try {
            val onboardingRepository = com.sovereign_rise.app.di.AppModule.provideOnboardingRepository(context)
            val isOnboardingCompleted = onboardingRepository.isOnboardingCompleted()
            if (isOnboardingCompleted) {
                val isTutorialCompleted = onboardingRepository.isTutorialCompleted()
                if (!isTutorialCompleted) {
                    tutorialState = onboardingRepository.getTutorialState()
                }
            }
        } catch (e: Exception) {
            // Silently fail - tutorial is optional
        }
    }
    
    // Load tasks when screen is displayed (handles navigation back from add/edit)
    LaunchedEffect(Unit) {
        if (uiState is TaskUiState.Loading || uiState is TaskUiState.TaskSaved) {
            viewModel.loadTasks()
        }
    }
    
    // Show Snackbar for state changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is TaskUiState.Error -> {
                val result = snackbarHostState.showSnackbar(
                    message = (uiState as TaskUiState.Error).message,
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.resetState()
                }
            }
            is TaskUiState.TaskDeleted -> {
                snackbarHostState.showSnackbar(
                    message = "Task deleted",
                    duration = SnackbarDuration.Short
                )
            }
            is TaskUiState.TaskSaved -> {
                snackbarHostState.showSnackbar(
                    message = "Task saved",
                    duration = SnackbarDuration.Short
                )
            }
            else -> {}
        }
        isRefreshing = false
    }
    
    // Show completion reward dialog
    completionResult?.let { result ->
        TaskCompletionDialog(
            result = result,
            onDismiss = { viewModel.clearCompletionResult() }
        )
    }
    
    // Show affirmation dialog (if no completion dialog)
    if (completionResult == null) {
        affirmation?.let { affirmationMessage ->
            AffirmationDialog(
                affirmation = affirmationMessage,
                onDismiss = { viewModel.clearAffirmation() }
            )
        }
    }
    
    // Show tutorial limit dialog
    if (showTutorialLimitDialog) {
        TutorialLimitDialog(
            tutorialState = tutorialState,
            onDismiss = { showTutorialLimitDialog = false }
        )
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(
                        onClick = { 
                            isRefreshing = true
                            viewModel.refresh() 
                        },
                        modifier = Modifier.semantics { contentDescription = "Refresh tasks" }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        },
        floatingActionButton = {
            val haptic = LocalHapticFeedback.current
            FloatingActionButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Check tutorial limits before navigating
                    val currentState = uiState
                    if (currentState is TaskUiState.Success) {
                        val taskCount = currentState.tasks.size
                        tutorialState?.let { state ->
                            if (!state.isCompleted && taskCount >= state.maxTasksAllowed) {
                                showTutorialLimitDialog = true
                                return@FloatingActionButton
                            }
                        }
                    }
                    navController.navigate(Screen.AddTask.route) 
                },
                containerColor = Primary,
                modifier = Modifier
                    .size(64.dp)
                    .semantics { contentDescription = "Add new task" },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = Elevation.high)
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add Task",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is TaskUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Primary
                    )
                }
                is TaskUiState.Empty -> {
                    EmptyTasksView(
                        onCreateTask = { navController.navigate(Screen.AddTask.route) }
                    )
                }
                is TaskUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sync status bar
                        SyncStatusBar(
                            syncState = syncState,
                            onRetry = { viewModel.retryFailedSync() }
                        )
                        
                        PullToRefreshContainer(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                viewModel.refresh()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            TaskList(
                                tasksWithStatus = state.tasks,
                                onTaskClick = { task ->
                                    viewModel.selectTask(task.id)
                                    navController.navigate(Screen.EditTask.createRoute(task.id))
                                },
                                onTaskComplete = { taskId ->
                                    viewModel.completeTask(taskId)
                                },
                                onTaskDelete = { taskId ->
                                    viewModel.deleteTask(taskId)
                                }
                            )
                        }
                    }
                }
                is TaskUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = Danger,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.resetState() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Retry")
                        }
                    }
                }
                is TaskUiState.TaskDeleted,
                is TaskUiState.TaskSaved -> {
                    // Show loading or nothing, Snackbar will handle feedback
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksView(
    onCreateTask: () -> Unit
) {
    EmptyStateView(
        icon = Icons.Outlined.TaskAlt,
        title = "No tasks yet",
        subtitle = "Start small. Even one task completed is progress.",
        actionLabel = "Create Task",
        onAction = onCreateTask,
        quote = "The journey of a thousand miles begins with a single step.",
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun TaskList(
    tasksWithStatus: List<com.sovereign_rise.app.presentation.viewmodel.TaskWithSyncStatus>,
    onTaskClick: (Task) -> Unit,
    onTaskComplete: (String) -> Unit,
    onTaskDelete: (String) -> Unit
) {
    // Group tasks by status using remember for performance
    val groupedTasks = remember(tasksWithStatus) {
        mapOf(
            "pending" to tasksWithStatus.filter { it.task.status == TaskStatus.PENDING },
            "completed" to tasksWithStatus.filter { it.task.status == TaskStatus.COMPLETED },
            "failed" to tasksWithStatus.filter { it.task.status == TaskStatus.FAILED }
        )
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing)
    ) {
        // Pending tasks section
        groupedTasks["pending"]?.let { pendingTasks ->
            if (pendingTasks.isNotEmpty()) {
                item(key = "pending_header") {
                    SectionHeader("Pending (${pendingTasks.size})")
                }
                items(
                    items = pendingTasks,
                    key = { it.task.id },
                    contentType = { "task" }
                ) { taskWithStatus ->
                    TaskItem(
                        task = taskWithStatus.task,
                        syncStatus = taskWithStatus.syncStatus,
                        onClick = { onTaskClick(taskWithStatus.task) },
                        onComplete = { onTaskComplete(taskWithStatus.task.id) },
                        onDelete = { onTaskDelete(taskWithStatus.task.id) }
                    )
                }
            }
        }
        
        // Completed tasks section
        groupedTasks["completed"]?.let { completedTasks ->
            if (completedTasks.isNotEmpty()) {
                item(key = "completed_header") {
                    Spacer(modifier = Modifier.height(Spacing.sectionSpacing))
                    SectionHeader("Completed (${completedTasks.size})")
                }
                items(
                    items = completedTasks,
                    key = { it.task.id },
                    contentType = { "task" }
                ) { taskWithStatus ->
                    TaskItem(
                        task = taskWithStatus.task,
                        syncStatus = taskWithStatus.syncStatus,
                        onClick = { onTaskClick(taskWithStatus.task) },
                        onComplete = { },
                        onDelete = { onTaskDelete(taskWithStatus.task.id) }
                    )
                }
            }
        }
        
        // Failed tasks section
        groupedTasks["failed"]?.let { failedTasks ->
            if (failedTasks.isNotEmpty()) {
                item(key = "failed_header") {
                    Spacer(modifier = Modifier.height(Spacing.sectionSpacing))
                    SectionHeader("Failed (${failedTasks.size})")
                }
                items(
                    items = failedTasks,
                    key = { it.task.id },
                    contentType = { "task" }
                ) { taskWithStatus ->
                    TaskItem(
                        task = taskWithStatus.task,
                        syncStatus = taskWithStatus.syncStatus,
                        onClick = { onTaskClick(taskWithStatus.task) },
                        onComplete = { },
                        onDelete = { onTaskDelete(taskWithStatus.task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = Typography.titleLarge,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = Spacing.extraSmall)
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TaskItem(
    task: Task,
    syncStatus: com.sovereign_rise.app.domain.model.SyncStatus,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart && task.status == TaskStatus.PENDING) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onComplete()
                true
            } else {
                false
            }
        }
    )
    
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Task",
            message = "Are you sure you want to delete this task?",
            confirmText = "Delete",
            onConfirm = onDelete,
            onDismiss = { showDeleteDialog = false },
            isDangerous = true
        )
    }
    
    // Only enable swipe for pending tasks
    if (task.status == TaskStatus.PENDING) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.cardPadding)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Success.copy(alpha = 0.1f),
                                    Success.copy(alpha = 0.3f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        modifier = Modifier.padding(Spacing.default)
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = "Complete",
                            tint = Success,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "Complete",
                            style = Typography.labelLarge,
                            color = Success,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) {
            TaskItemContent(
                task = task,
                syncStatus = syncStatus,
                onClick = onClick,
                onComplete = onComplete,
                onDelete = { showDeleteDialog = true }
            )
        }
    } else {
        TaskItemContent(
            task = task,
            syncStatus = syncStatus,
            onClick = onClick,
            onComplete = onComplete,
            onDelete = { showDeleteDialog = true }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskItemContent(
    task: Task,
    syncStatus: com.sovereign_rise.app.domain.model.SyncStatus,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isCompleted = task.status == TaskStatus.COMPLETED
    val isPending = task.status == TaskStatus.PENDING
    
    ModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .optimizedListItem()
            .semantics { 
                contentDescription = "Task: ${task.title}, ${task.difficulty.name} difficulty, Status: ${task.status.name}"
            },
        onClick = onClick,
        cornerRadius = 16.dp,
        useGlassmorphism = isCompleted,
        gradientBorder = false,
        elevation = Elevation.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox for pending tasks
            if (isPending) {
                Checkbox(
                    checked = false,
                    onCheckedChange = { 
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onComplete()
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Success,
                        uncheckedColor = TextTertiary
                    ),
                    modifier = Modifier
                        .size(28.dp)
                        .semantics { 
                            contentDescription = "Mark task as complete"
                        }
                )
                Spacer(modifier = Modifier.width(Spacing.small))
            } else if (isCompleted) {
                Checkbox(
                    checked = true,
                    onCheckedChange = null,
                    enabled = false,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Success,
                        disabledCheckedColor = Success
                    ),
                    modifier = Modifier
                        .size(28.dp)
                        .semantics { 
                            contentDescription = "Task completed"
                        }
                )
                Spacer(modifier = Modifier.width(Spacing.small))
            }
            
            // Task Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Task name and sync status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = Typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Sync status indicator
                    com.sovereign_rise.app.presentation.components.SyncIndicator(
                        syncState = when (syncStatus) {
                            com.sovereign_rise.app.domain.model.SyncStatus.SYNCED -> SyncState.Synced
                            com.sovereign_rise.app.domain.model.SyncStatus.PENDING -> SyncState.Pending(1)
                            com.sovereign_rise.app.domain.model.SyncStatus.SYNCING -> SyncState.Syncing(0, 1)
                            com.sovereign_rise.app.domain.model.SyncStatus.FAILED -> SyncState.Failed(1, emptyList())
                            com.sovereign_rise.app.domain.model.SyncStatus.CONFLICT -> SyncState.Failed(1, listOf("Conflict"))
                        },
                        compact = true,
                        showLabel = false
                    )
                }
                
                // Description
                if (task.description != null && task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Difficulty and reminder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Difficulty chip
                    DifficultyChip(difficulty = task.difficulty)
                    
                    // Reminder time if set
                    if (task.reminderTime != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = TextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDateTime(task.reminderTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
            
            // Action button (Delete only - checkbox handles completion)
            IconButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Danger,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: TaskStatus) {
    val statusPair: Pair<androidx.compose.ui.graphics.Color, String> = when (status) {
        TaskStatus.PENDING -> Accent to "Pending"
        TaskStatus.COMPLETED -> Success to "Completed"
        TaskStatus.PARTIAL -> Warning to "Partial"
        TaskStatus.FAILED -> Error to "Failed"
    }
    val (color, text) = statusPair
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DifficultyChip(difficulty: TaskDifficulty) {
    val (color, text, icon) = when (difficulty) {
        TaskDifficulty.EASY -> Triple(Success, "Easy", Icons.Outlined.SentimentSatisfied)
        TaskDifficulty.MEDIUM -> Triple(Warning, "Medium", Icons.Outlined.SentimentNeutral)
        TaskDifficulty.HARD -> Triple(Color(0xFFFF6B35), "Hard", Icons.Outlined.SentimentDissatisfied)
        TaskDifficulty.VERY_HARD -> Triple(Error, "Very Hard", Icons.Outlined.Whatshot)
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = Typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TaskCompletionDialog(
    result: Task,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Task Completed!",
                    style = Typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Great work! Keep up the momentum!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Success
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Awesome!")
            }
        }
    )
}

private fun formatDateTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
private fun TutorialLimitDialog(
    tutorialState: com.sovereign_rise.app.domain.model.TutorialState?,
    onDismiss: () -> Unit
) {
    if (tutorialState == null) {
        onDismiss()
        return
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Tutorial Limit Reached",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column {
                Text("You've reached the task limit for ${tutorialState.currentDay.name.replace("_", " ")}.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current limit: ${tutorialState.maxTasksAllowed} tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Complete your tutorial tasks to unlock more capacity and progress to the next day!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Got it")
            }
        }
    )
}

