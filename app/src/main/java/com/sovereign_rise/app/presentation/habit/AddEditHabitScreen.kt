package com.sovereign_rise.app.presentation.habit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sovereign_rise.app.domain.model.HabitType
import com.sovereign_rise.app.presentation.components.*
import com.sovereign_rise.app.presentation.navigation.Screen
import com.sovereign_rise.app.presentation.viewmodel.HabitUiState
import com.sovereign_rise.app.presentation.viewmodel.HabitViewModel
import com.sovereign_rise.app.ui.theme.*
import com.sovereign_rise.app.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitScreen(
    navController: NavController,
    viewModel: HabitViewModel,
    habitId: String? = null
) {
    val isEditMode = habitId != null
    val uiState by viewModel.uiState.collectAsState()
    val selectedHabit by viewModel.selectedHabit.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Form state
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var habitType by remember { mutableStateOf(HabitType.DAILY) }
    var intervalDays by remember { mutableStateOf("1") }
    var isActive by remember { mutableStateOf(true) }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var intervalError by remember { mutableStateOf<String?>(null) }
    
    // Track original values for unsaved changes detection
    var originalName by remember { mutableStateOf("") }
    var originalDescription by remember { mutableStateOf("") }
    var originalHabitType by remember { mutableStateOf(HabitType.DAILY) }
    var originalIntervalDays by remember { mutableStateOf("1") }
    var originalIsActive by remember { mutableStateOf(true) }
    
    // Unsaved changes state
    val hasUnsavedChanges = remember(name, description, habitType, intervalDays, isActive) {
        name != originalName || 
        description != originalDescription || 
        habitType != originalHabitType || 
        intervalDays != originalIntervalDays ||
        isActive != originalIsActive
    }
    
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showTemplatesDialog by remember { mutableStateOf(false) }
    
    // Load habit data when habitId is provided (e.g., from deep link or config change)
    LaunchedEffect(habitId) {
        if (habitId != null) {
            viewModel.selectHabit(habitId)
        }
    }
    
    // Populate form when habit is loaded
    LaunchedEffect(selectedHabit) {
        selectedHabit?.let { habit ->
            name = habit.name
            description = habit.description ?: ""
            habitType = habit.type
            intervalDays = habit.intervalDays.toString()
            isActive = habit.isActive
            
            // Store original values
            originalName = habit.name
            originalDescription = habit.description ?: ""
            originalHabitType = habit.type
            originalIntervalDays = habit.intervalDays.toString()
            originalIsActive = habit.isActive
        }
    }
    
    // Track if save button was clicked to differentiate from initial Success state
    var saveClicked by remember { mutableStateOf(false) }
    
    // Handle errors
    LaunchedEffect(uiState) {
        if (uiState is HabitUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as HabitUiState.Error).message,
                duration = SnackbarDuration.Long
            )
        }
    }
    
    // Handle navigation only after save is clicked and state is Success
    if (uiState is HabitUiState.Success && saveClicked) {
        LaunchedEffect(key1 = "navigate_back") {
            // Navigate to Habits screen after saving (not popBackStack to avoid going to Home)
            navController.navigate(Screen.Habits.route) {
                popUpTo(Screen.Habits.route) { 
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
    
    // Habit templates dialog
    if (showTemplatesDialog) {
        HabitTemplatesDialog(
            onDismiss = { showTemplatesDialog = false },
            onTemplateSelected = { template ->
                name = template.name
                description = template.description
                habitType = template.type
                showTemplatesDialog = false
            }
        )
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedHabit()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isEditMode) "Edit Habit" else "Add Habit",
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
                            
                            if (name.isBlank()) {
                                nameError = "Name is required"
                                hasErrors = true
                            } else if (name.length < Constants.MIN_HABIT_NAME_LENGTH) {
                                nameError = Constants.ERROR_HABIT_NAME_TOO_SHORT
                                hasErrors = true
                            } else if (name.length > Constants.MAX_HABIT_NAME_LENGTH) {
                                nameError = Constants.ERROR_HABIT_NAME_TOO_LONG
                                hasErrors = true
                            } else {
                                nameError = null
                            }
                            
                            if (description.length > Constants.MAX_HABIT_DESCRIPTION_LENGTH) {
                                descriptionError = Constants.ERROR_HABIT_DESCRIPTION_TOO_LONG
                                hasErrors = true
                            } else {
                                descriptionError = null
                            }
                            
                            val interval = intervalDays.toIntOrNull()
                            if (habitType == HabitType.CUSTOM_INTERVAL && (interval == null || interval < Constants.MIN_CUSTOM_INTERVAL_DAYS)) {
                                intervalError = Constants.ERROR_INVALID_INTERVAL
                                hasErrors = true
                            } else {
                                intervalError = null
                            }
                            
                            if (!hasErrors) {
                                val finalInterval = when (habitType) {
                                    HabitType.DAILY -> 1
                                    HabitType.WEEKLY -> 7
                                    HabitType.CUSTOM_INTERVAL -> intervalDays.toIntOrNull() ?: 1
                                }
                                
                                // Set flag to indicate save button was clicked
                                saveClicked = true
                                
                                if (isEditMode && habitId != null) {
                                    viewModel.updateHabit(
                                        habitId = habitId,
                                        name = name,
                                        description = description.ifBlank { null },
                                        type = habitType,
                                        intervalDays = finalInterval,
                                        isActive = isActive
                                    )
                                } else {
                                    viewModel.createHabit(
                                        name = name,
                                        description = description.ifBlank { null },
                                        type = habitType,
                                        intervalDays = finalInterval
                                    )
                                }
                            }
                        },
                        enabled = uiState !is HabitUiState.Loading,
                        modifier = Modifier.semantics { contentDescription = "Save habit" }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = if (uiState is HabitUiState.Loading) TextDisabled else Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Save",
                                style = Typography.labelLarge,
                                color = if (uiState is HabitUiState.Loading) TextDisabled else Primary
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
                // Show habit templates button (only in add mode)
                if (!isEditMode) {
                    OutlinedButton(
                        onClick = { showTemplatesDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .semantics { contentDescription = "Browse habit templates" },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text("Browse Templates", style = Typography.labelLarge)
                    }
                }
                // Name field with modern styling
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text("Habit Name *", style = Typography.bodyMedium) },
                    placeholder = { Text("What habit do you want to build?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .semantics { contentDescription = "Habit name input" },
                    isError = nameError != null,
                    supportingText = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (nameError != null) {
                                Text(
                                    nameError!!,
                                    color = Error,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            Text(
                                "${name.length}/${Constants.MAX_HABIT_NAME_LENGTH}",
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
                    placeholder = { Text("Why is this habit important?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
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
                                "${description.length}/${Constants.MAX_HABIT_DESCRIPTION_LENGTH}",
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
                
                // Frequency selector with modern design
                ModernCard(
                    modifier = Modifier.fillMaxWidth(),
                    useGlassmorphism = false,
                    elevation = Elevation.medium
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(Spacing.default)
                    ) {
                        Text(
                            text = "Frequency",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            FrequencyButton(
                                type = HabitType.DAILY,
                                label = "Daily",
                                icon = Icons.Outlined.Today,
                                isSelected = habitType == HabitType.DAILY,
                                onClick = { habitType = HabitType.DAILY },
                                modifier = Modifier.weight(1f)
                            )
                            FrequencyButton(
                                type = HabitType.WEEKLY,
                                label = "Weekly",
                                icon = Icons.Outlined.CalendarMonth,
                                isSelected = habitType == HabitType.WEEKLY,
                                onClick = { habitType = HabitType.WEEKLY },
                                modifier = Modifier.weight(1f)
                            )
                            FrequencyButton(
                                type = HabitType.CUSTOM_INTERVAL,
                                label = "Custom",
                                icon = Icons.Outlined.CalendarToday,
                                isSelected = habitType == HabitType.CUSTOM_INTERVAL,
                                onClick = { habitType = HabitType.CUSTOM_INTERVAL },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Interval Days field (only for Custom type)
                androidx.compose.animation.AnimatedVisibility(visible = habitType == HabitType.CUSTOM_INTERVAL) {
                    OutlinedTextField(
                        value = intervalDays,
                        onValueChange = { 
                            intervalDays = it.filter { char -> char.isDigit() }
                            intervalError = null
                        },
                        label = { Text("Repeat every X days", style = Typography.bodyMedium) },
                        placeholder = { Text("E.g., 3 for every 3 days") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = intervalError != null,
                        supportingText = {
                            if (intervalError != null) {
                                Text(intervalError!!, color = Error, style = Typography.bodySmall)
                            } else {
                                Text("Minimum: ${Constants.MIN_CUSTOM_INTERVAL_DAYS} days", style = Typography.bodySmall, color = TextTertiary)
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
                        textStyle = Typography.bodyLarge.copy(fontFamily = MonoFontFamily)
                    )
                }
                
                // Active/Inactive toggle (edit mode only)
                if (isEditMode) {
                    ModernCard(
                        modifier = Modifier.fillMaxWidth(),
                        useGlassmorphism = false,
                        elevation = Elevation.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.cardPadding),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isActive) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                                        contentDescription = null,
                                        tint = if (isActive) Success else TextTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Active",
                                        style = Typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "Inactive habits won't be checked for streak breaks",
                                    style = Typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = isActive,
                                onCheckedChange = { isActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Success,
                                    checkedTrackColor = Success.copy(alpha = 0.5f),
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = SurfaceVariant
                                )
                            )
                        }
                    }
                }
                
                // Save button (alternative to top bar)
                Button(
                    onClick = {
                        // Same validation logic as top bar save button
                        var hasErrors = false
                        
                        if (name.isBlank()) {
                            nameError = "Name is required"
                            hasErrors = true
                        } else if (name.length < Constants.MIN_HABIT_NAME_LENGTH) {
                            nameError = Constants.ERROR_HABIT_NAME_TOO_SHORT
                            hasErrors = true
                        } else if (name.length > Constants.MAX_HABIT_NAME_LENGTH) {
                            nameError = Constants.ERROR_HABIT_NAME_TOO_LONG
                            hasErrors = true
                        } else {
                            nameError = null
                        }
                        
                        if (description.length > Constants.MAX_HABIT_DESCRIPTION_LENGTH) {
                            descriptionError = Constants.ERROR_HABIT_DESCRIPTION_TOO_LONG
                            hasErrors = true
                        } else {
                            descriptionError = null
                        }
                        
                        val interval = intervalDays.toIntOrNull()
                        if (habitType == HabitType.CUSTOM_INTERVAL && (interval == null || interval < Constants.MIN_CUSTOM_INTERVAL_DAYS)) {
                            intervalError = Constants.ERROR_INVALID_INTERVAL
                            hasErrors = true
                        } else {
                            intervalError = null
                        }
                        
                        if (!hasErrors) {
                            val finalInterval = when (habitType) {
                                HabitType.DAILY -> 1
                                HabitType.WEEKLY -> 7
                                HabitType.CUSTOM_INTERVAL -> intervalDays.toIntOrNull() ?: 1
                            }
                            
                            if (isEditMode && habitId != null) {
                                viewModel.updateHabit(
                                    habitId = habitId,
                                    name = name,
                                    description = description.ifBlank { null },
                                    type = habitType,
                                    intervalDays = finalInterval,
                                    isActive = isActive
                                )
                            } else {
                                viewModel.createHabit(
                                    name = name,
                                    description = description.ifBlank { null },
                                    type = habitType,
                                    intervalDays = finalInterval
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = if (isEditMode) "Update habit button" else "Create habit button" },
                    enabled = uiState !is HabitUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(if (isEditMode) "Update Habit" else "Create Habit")
                }
            }
            
            // Loading Overlay
            LoadingOverlay(
                isLoading = uiState is HabitUiState.Loading,
                message = if (isEditMode) "Updating habit..." else "Creating habit..."
            )
        }
    }
}

@Composable
private fun FrequencyButton(
    type: HabitType,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    val color = when (type) {
        HabitType.DAILY -> Primary
        HabitType.WEEKLY -> Secondary
        HabitType.CUSTOM_INTERVAL -> Tertiary
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "frequency_scale"
    )
    
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .height(56.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
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
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(Spacing.small)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = Typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private data class HabitTemplate(
    val name: String,
    val description: String,
    val type: HabitType
)

@Composable
private fun HabitTemplatesDialog(
    onDismiss: () -> Unit,
    onTemplateSelected: (HabitTemplate) -> Unit
) {
    val templates = remember {
        listOf(
            HabitTemplate("Morning Exercise", "Start your day with 30 minutes of physical activity", HabitType.DAILY),
            HabitTemplate("Meditation", "Practice mindfulness and reduce stress", HabitType.DAILY),
            HabitTemplate("Read for 30 minutes", "Expand your knowledge and imagination", HabitType.DAILY),
            HabitTemplate("Drink 8 glasses of water", "Stay hydrated throughout the day", HabitType.DAILY),
            HabitTemplate("Journal", "Reflect on your day and practice gratitude", HabitType.DAILY),
            HabitTemplate("Learn a new skill", "Spend time improving yourself", HabitType.DAILY),
            HabitTemplate("Call family/friends", "Maintain important relationships", HabitType.WEEKLY),
            HabitTemplate("Deep clean living space", "Keep your environment organized", HabitType.WEEKLY),
            HabitTemplate("Review weekly goals", "Track progress and adjust plans", HabitType.WEEKLY),
            HabitTemplate("Cook a healthy meal", "Nourish your body with home-cooked food", HabitType.DAILY)
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Habit Templates", style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates.size) { index ->
                    val template = templates[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTemplateSelected(template) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = when (template.type) {
                                        HabitType.DAILY -> Success.copy(alpha = 0.2f)
                                        HabitType.WEEKLY -> Info.copy(alpha = 0.2f)
                                        else -> TextMuted.copy(alpha = 0.2f)
                                    },
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = when (template.type) {
                                            HabitType.DAILY -> "Daily"
                                            HabitType.WEEKLY -> "Weekly"
                                            else -> "Custom"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = when (template.type) {
                                            HabitType.DAILY -> Success
                                            HabitType.WEEKLY -> Info
                                            else -> TextMuted
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = template.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

