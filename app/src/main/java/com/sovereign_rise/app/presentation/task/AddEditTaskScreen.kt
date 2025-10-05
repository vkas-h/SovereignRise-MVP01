package com.sovereign_rise.app.presentation.task

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sovereign_rise.app.domain.model.TaskDifficulty
import com.sovereign_rise.app.presentation.components.*
import com.sovereign_rise.app.presentation.navigation.Screen
import com.sovereign_rise.app.presentation.viewmodel.TaskUiState
import com.sovereign_rise.app.presentation.viewmodel.TaskViewModel
import com.sovereign_rise.app.ui.theme.*
import com.sovereign_rise.app.util.Constants
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    navController: NavController,
    viewModel: TaskViewModel,
    taskId: String? = null
) {
    val isEditMode = taskId != null
    val uiState by viewModel.uiState.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Form state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(TaskDifficulty.MEDIUM) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    
    var titleError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var reminderError by remember { mutableStateOf<String?>(null) }
    
    // Track original values for unsaved changes detection
    var originalTitle by remember { mutableStateOf("") }
    var originalDescription by remember { mutableStateOf("") }
    var originalDifficulty by remember { mutableStateOf(TaskDifficulty.MEDIUM) }
    var originalReminderTime by remember { mutableStateOf<Long?>(null) }
    
    // Unsaved changes state
    val hasUnsavedChanges = remember(title, description, difficulty, reminderTime) {
        title != originalTitle || 
        description != originalDescription || 
        difficulty != originalDifficulty || 
        reminderTime != originalReminderTime
    }
    
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    
    // Load task for editing
    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.selectTask(taskId)
        }
    }
    
    // Populate form when task is loaded
    LaunchedEffect(selectedTask) {
        selectedTask?.let { task ->
            title = task.title
            description = task.description ?: ""
            difficulty = task.difficulty
            reminderTime = task.reminderTime
            
            // Store original values
            originalTitle = task.title
            originalDescription = task.description ?: ""
            originalDifficulty = task.difficulty
            originalReminderTime = task.reminderTime
        }
    }
    
    // Handle errors
    LaunchedEffect(uiState) {
        if (uiState is TaskUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as TaskUiState.Error).message,
                duration = SnackbarDuration.Long
            )
        }
    }
    
    // Handle navigation separately with specific key to prevent re-runs
    if (uiState is TaskUiState.TaskSaved) {
        LaunchedEffect(key1 = "navigate_back") {
            // Navigate to Tasks screen after saving (not popBackStack to avoid going to Home)
            navController.navigate(Screen.Tasks.route) {
                popUpTo(Screen.Tasks.route) { 
                    inclusive = true 
                }
                launchSingleTop = true
            }
        }
    }
    
    // Handle back press with unsaved changes warning
    BackHandler(enabled = hasUnsavedChanges) {
        showUnsavedChangesDialog = true
    }
    
    // Unsaved changes dialog
    if (showUnsavedChangesDialog) {
        ConfirmationDialog(
            title = "Unsaved Changes",
            message = "You have unsaved changes. Are you sure you want to discard them?",
            confirmText = "Discard",
            onConfirm = {
                showUnsavedChangesDialog = false
                navController.popBackStack()
            },
            onDismiss = { showUnsavedChangesDialog = false },
            isDangerous = true
        )
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedTask()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isEditMode) "Edit Task" else "Add Task",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasUnsavedChanges) {
                                showUnsavedChangesDialog = true
                            } else {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Go back" }
                    ) {
                        Icon(
                            Icons.Outlined.ArrowBack, 
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // Validate
                            var hasErrors = false
                            
                            if (title.isBlank()) {
                                titleError = "Title is required"
                                hasErrors = true
                            } else if (title.length < Constants.MIN_TASK_TITLE_LENGTH) {
                                titleError = Constants.ERROR_TASK_TITLE_TOO_SHORT
                                hasErrors = true
                            } else if (title.length > Constants.MAX_TASK_TITLE_LENGTH) {
                                titleError = Constants.ERROR_TASK_TITLE_TOO_LONG
                                hasErrors = true
                            } else {
                                titleError = null
                            }
                            
                            if (description.length > Constants.MAX_TASK_DESCRIPTION_LENGTH) {
                                descriptionError = Constants.ERROR_TASK_DESCRIPTION_TOO_LONG
                                hasErrors = true
                            } else {
                                descriptionError = null
                            }
                            
                            if (reminderTime != null && reminderTime!! <= System.currentTimeMillis()) {
                                reminderError = Constants.ERROR_REMINDER_IN_PAST
                                hasErrors = true
                            } else {
                                reminderError = null
                            }
                            
                            if (!hasErrors) {
                                if (isEditMode && taskId != null) {
                                    viewModel.updateTask(
                                        taskId = taskId,
                                        title = title,
                                        description = description.ifBlank { null },
                                        difficulty = difficulty,
                                        reminderTime = reminderTime
                                    )
                                } else {
                                    viewModel.createTask(
                                        title = title,
                                        description = description.ifBlank { null },
                                        difficulty = difficulty,
                                        reminderTime = reminderTime
                                    )
                                }
                            }
                        },
                        enabled = uiState !is TaskUiState.Loading,
                        modifier = Modifier.semantics { contentDescription = "Save task" }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = if (uiState is TaskUiState.Loading) TextDisabled else Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Save",
                                style = Typography.labelLarge,
                                color = if (uiState is TaskUiState.Loading) TextDisabled else Primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundPrimary
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.screenPadding)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.large)
            ) {
                // Title field with modern styling
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        titleError = null
                    },
                    label = { Text("Task Title *", style = Typography.bodyMedium) },
                    placeholder = { Text("What do you want to accomplish?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .semantics { contentDescription = "Task title input" },
                    isError = titleError != null,
                    supportingText = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (titleError != null) {
                                Text(
                                    titleError!!,
                                    color = Error,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            Text(
                                "${title.length}/${Constants.MAX_TASK_TITLE_LENGTH}",
                                style = Typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                color = TextTertiary
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = BorderMedium,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = TextSecondary
                    ),
                    textStyle = Typography.bodyLarge
                )
                
                // Description field with modern styling
                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        descriptionError = null
                    },
                    label = { Text("Description (Optional)", style = Typography.bodyMedium) },
                    placeholder = { Text("Add details about this task...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6,
                    isError = descriptionError != null,
                    supportingText = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (descriptionError != null) {
                                Text(
                                    descriptionError!!,
                                    color = Error,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            Text(
                                "${description.length}/${Constants.MAX_TASK_DESCRIPTION_LENGTH}",
                                style = Typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                color = TextTertiary
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = BorderMedium,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = TextSecondary
                    ),
                    textStyle = Typography.bodyLarge
                )
                
                // Difficulty selector with modern design
                ModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    useGlassmorphism = false,
                    elevation = Elevation.medium
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(Spacing.default)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Difficulty",
                                style = Typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            // XP Preview
}
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            ModernDifficultyButton(
                                difficulty = TaskDifficulty.EASY,
                                isSelected = difficulty == TaskDifficulty.EASY,
                                onClick = { difficulty = TaskDifficulty.EASY },
                                modifier = Modifier.weight(1f)
                            )
                            ModernDifficultyButton(
                                difficulty = TaskDifficulty.MEDIUM,
                                isSelected = difficulty == TaskDifficulty.MEDIUM,
                                onClick = { difficulty = TaskDifficulty.MEDIUM },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            ModernDifficultyButton(
                                difficulty = TaskDifficulty.HARD,
                                isSelected = difficulty == TaskDifficulty.HARD,
                                onClick = { difficulty = TaskDifficulty.HARD },
                                modifier = Modifier.weight(1f)
                            )
                            ModernDifficultyButton(
                                difficulty = TaskDifficulty.VERY_HARD,
                                isSelected = difficulty == TaskDifficulty.VERY_HARD,
                                onClick = { difficulty = TaskDifficulty.VERY_HARD },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Reminder time picker
                ReminderTimePicker(
                    reminderTime = reminderTime,
                    onReminderTimeChange = { 
                        reminderTime = it
                        reminderError = null
                    },
                    error = reminderError
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Loading Overlay
            LoadingOverlay(
                isLoading = uiState is TaskUiState.Loading,
                message = if (isEditMode) "Updating task..." else "Creating task..."
            )
        }
    }
}

@Composable
private fun ModernDifficultyButton(
    difficulty: TaskDifficulty,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    val (color, text, icon) = when (difficulty) {
        TaskDifficulty.EASY -> Triple(Success, "Easy", Icons.Outlined.SentimentSatisfied)
        TaskDifficulty.MEDIUM -> Triple(Warning, "Medium", Icons.Outlined.SentimentNeutral)
        TaskDifficulty.HARD -> Triple(androidx.compose.ui.graphics.Color(0xFFFF6B35), "Hard", Icons.Outlined.SentimentDissatisfied)
        TaskDifficulty.VERY_HARD -> Triple(Error, "Very Hard", Icons.Outlined.Whatshot)
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "difficulty_scale"
    )
    
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .height(56.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .semantics { contentDescription = "Select $text difficulty" }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f))),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else color.copy(alpha = 0.15f),
            contentColor = if (isSelected) androidx.compose.ui.graphics.Color.White else color
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = Typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ReminderTimePicker(
    reminderTime: Long?,
    onReminderTimeChange: (Long?) -> Unit,
    error: String?
) {
    val context = LocalContext.current
    
    Column {
        Text(
            text = "Reminder (Optional)",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = {
                val calendar = Calendar.getInstance()
                if (reminderTime != null) {
                    calendar.timeInMillis = reminderTime
                }
                
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val timeCalendar = Calendar.getInstance()
                        timeCalendar.set(year, month, dayOfMonth)
                        
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                timeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                timeCalendar.set(Calendar.MINUTE, minute)
                                onReminderTimeChange(timeCalendar.timeInMillis)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (reminderTime != null) Primary else TextMuted
            )
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (reminderTime != null) {
                    formatDateTime(reminderTime)
                } else {
                    "Set Reminder"
                }
            )
            if (reminderTime != null) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onReminderTimeChange(null) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = Danger,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

private fun formatDateTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

