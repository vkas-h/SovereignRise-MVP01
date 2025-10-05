package com.sovereign_rise.app.presentation.habit

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sovereign_rise.app.domain.model.Habit
import com.sovereign_rise.app.domain.model.HabitType
import com.sovereign_rise.app.domain.model.TutorialDay
import com.sovereign_rise.app.presentation.components.*
import com.sovereign_rise.app.presentation.navigation.Screen
import com.sovereign_rise.app.presentation.viewmodel.HabitUiState
import com.sovereign_rise.app.presentation.viewmodel.HabitViewModel
import com.sovereign_rise.app.ui.theme.*
import com.sovereign_rise.app.util.Constants
import com.sovereign_rise.app.util.optimizedListItem
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    navController: NavController,
    viewModel: HabitViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val tickResult by viewModel.tickResult.collectAsState()
    val streakBreakResult by viewModel.streakBreakResult.collectAsState()
    val affirmation by viewModel.affirmation.collectAsState()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Tutorial state
    var tutorialState by remember { mutableStateOf<com.sovereign_rise.app.domain.model.TutorialState?>(null) }
    var showHabitLockedDialog by remember { mutableStateOf(false) }
    
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
    
    // Load habits when screen is displayed (handles navigation back from add/edit)
    LaunchedEffect(Unit) {
        if (uiState is HabitUiState.Loading || uiState is HabitUiState.Success) {
            // Reload habits if we're in loading or if returning from add/edit
            val currentState = uiState
            if (currentState is HabitUiState.Success && currentState.habits.isEmpty()) {
                viewModel.loadHabits()
            }
        }
    }
    
    // Show Snackbar for state changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is HabitUiState.Error -> {
                val result = snackbarHostState.showSnackbar(
                    message = (uiState as HabitUiState.Error).message,
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.resetState()
                }
            }
            else -> {}
        }
        isRefreshing = false
    }
    
    // Show Snackbar when habit is ticked (brief notification)
    LaunchedEffect(tickResult) {
        tickResult?.let { result ->
            if (result.milestoneAchieved == null) {
                snackbarHostState.showSnackbar(
                    message = "Habit checked! Keep building that streak! 🔥",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }
    
    // Show tick reward dialog (if milestone achieved)
    tickResult?.let { result ->
        if (result.milestoneAchieved != null) {
            HabitTickDialog(
                result = result,
                onDismiss = { viewModel.clearTickResult() }
            )
        }
    }
    
    // Show streak break dialog (if no tick dialog)
    if (tickResult == null) {
        streakBreakResult?.let { result ->
            StreakBreakDialog(
                result = result,
                onDismiss = { viewModel.clearStreakBreakResult() }
            )
        }
    }
    
    // Show affirmation dialog (if no other dialogs)
    if (tickResult?.milestoneAchieved == null && streakBreakResult == null) {
        affirmation?.let { affirmationMessage ->
            AffirmationDialog(
                affirmation = affirmationMessage,
                onDismiss = { viewModel.clearAffirmation() }
            )
        }
    }
    
    // Show habit locked dialog (tutorial day 1)
    if (showHabitLockedDialog) {
        HabitLockedDialog(
            onDismiss = { showHabitLockedDialog = false }
        )
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Habits") },
                actions = {
                    IconButton(
                        onClick = { 
                            isRefreshing = true
                            viewModel.refresh() 
                        },
                        modifier = Modifier.semantics { contentDescription = "Refresh habits" }
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
                    // Check if habits are unlocked in tutorial
                    tutorialState?.let { state ->
                        if (!state.isCompleted) {
                            // Habits are unlocked on DAY_2 and beyond
                            if (state.currentDay == TutorialDay.DAY_1) {
                                showHabitLockedDialog = true
                                return@FloatingActionButton
                            }
                        }
                    }
                    navController.navigate(Screen.AddHabit.route) 
                },
                containerColor = Primary,
                modifier = Modifier.semantics { contentDescription = "Add new habit" }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
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
                is HabitUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Primary
                    )
                }
                is HabitUiState.Empty -> {
                    EmptyHabitsView(
                        onCreateHabit = { navController.navigate(Screen.AddHabit.route) }
                    )
                }
                is HabitUiState.Success -> {
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
                            HabitList(
                                habitsWithStatus = state.habits,
                                onHabitClick = { habit ->
                                    viewModel.selectHabit(habit)
                                    navController.navigate(Screen.EditHabit.createRoute(habit.id))
                                },
                                onHabitTick = { habitId ->
                                    viewModel.tickHabit(habitId)
                                },
                                onHabitDelete = { habitId ->
                                    viewModel.deleteHabit(habitId)
                                }
                            )
                        }
                    }
                }
                is HabitUiState.Error -> {
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
            }
        }
    }
}

@Composable
private fun EmptyHabitsView(
    onCreateHabit: () -> Unit
) {
    EmptyStateView(
        icon = Icons.Outlined.Loop,
        title = "No habits yet",
        subtitle = "Habits are the compound interest of self-improvement.",
        actionLabel = "Create Habit",
        onAction = onCreateHabit,
        quote = "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun HabitList(
    habitsWithStatus: List<com.sovereign_rise.app.presentation.viewmodel.HabitWithSyncStatus>,
    onHabitClick: (Habit) -> Unit,
    onHabitTick: (String) -> Unit,
    onHabitDelete: (String) -> Unit
) {
    // Group habits by active status using remember for performance
    val groupedHabits = remember(habitsWithStatus) {
        mapOf(
            "active" to habitsWithStatus.filter { it.habit.isActive },
            "inactive" to habitsWithStatus.filter { !it.habit.isActive }
        )
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing)
    ) {
        // Active Habits Section
        groupedHabits["active"]?.let { activeHabits ->
            if (activeHabits.isNotEmpty()) {
                item(key = "active_header") {
                    Text(
                        text = "Active Habits (${activeHabits.size})",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = Spacing.extraSmall)
                    )
                }
                items(
                    items = activeHabits,
                    key = { it.habit.id },
                    contentType = { "habit" }
                ) { habitWithStatus ->
                    HabitItem(
                        habit = habitWithStatus.habit,
                        syncStatus = habitWithStatus.syncStatus,
                        onClick = { onHabitClick(habitWithStatus.habit) },
                        onTick = { onHabitTick(habitWithStatus.habit.id) },
                        onDelete = { onHabitDelete(habitWithStatus.habit.id) }
                    )
                }
            }
        }
        
        // Inactive Habits Section
        groupedHabits["inactive"]?.let { inactiveHabits ->
            if (inactiveHabits.isNotEmpty()) {
                item(key = "inactive_header") {
                    Spacer(modifier = Modifier.height(Spacing.sectionSpacing))
                    Text(
                        text = "Inactive Habits (${inactiveHabits.size})",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = Spacing.extraSmall)
                    )
                }
                items(
                    items = inactiveHabits,
                    key = { it.habit.id },
                    contentType = { "habit" }
                ) { habitWithStatus ->
                    HabitItem(
                        habit = habitWithStatus.habit,
                        syncStatus = habitWithStatus.syncStatus,
                        onClick = { onHabitClick(habitWithStatus.habit) },
                        onTick = { onHabitTick(habitWithStatus.habit.id) },
                        onDelete = { onHabitDelete(habitWithStatus.habit.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitItem(
    habit: Habit,
    syncStatus: com.sovereign_rise.app.domain.model.SyncStatus,
    onClick: () -> Unit,
    onTick: () -> Unit,
    onDelete: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Habit?",
            message = "Are you sure you want to delete \"${habit.name}\"?",
            confirmText = "Delete",
            onConfirm = onDelete,
            onDismiss = { showDeleteDialog = false },
            isDangerous = true
        )
    }
    
    val isCheckedToday = habit.canCheckToday().not()
    val nextMilestone = habit.getNextMilestone() ?: 100
    val progress = habit.streakDays.toFloat() / nextMilestone.toFloat()
    
    // Animated scale for checkbox
    val checkboxScale by animateFloatAsState(
        targetValue = if (isCheckedToday) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkbox_scale"
    )
    
    ModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .optimizedListItem()
            .semantics { 
                contentDescription = "Habit: ${habit.name}, ${habit.streakDays} day streak, ${if (isCheckedToday) "Checked today" else "Not checked today"}"
            },
        onClick = onClick,
        useGlassmorphism = isCheckedToday,
        gradientBorder = habit.streakDays >= 30,
        elevation = Elevation.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox with scale animation
            Checkbox(
                checked = isCheckedToday,
                onCheckedChange = { 
                    if (!isCheckedToday) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTick()
                    }
                },
                enabled = !isCheckedToday && habit.isActive,
                colors = CheckboxDefaults.colors(
                    checkedColor = Success,
                    uncheckedColor = TextTertiary,
                    disabledCheckedColor = Success
                ),
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer(scaleX = checkboxScale, scaleY = checkboxScale)
                    .semantics { 
                        contentDescription = if (isCheckedToday) "Checked today" else "Check habit"
                    }
            )
            
            Spacer(modifier = Modifier.width(Spacing.small))
            
            // Habit Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Habit name and sync status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = habit.name,
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Sync status indicator
                    SyncIndicator(
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
                if (habit.description != null && habit.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = habit.description,
                        style = Typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(Spacing.small))
                
                // Habit type and streak info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type badge
                    Surface(
                        color = Primary.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = when (habit.type) {
                                HabitType.DAILY -> "Daily"
                                HabitType.WEEKLY -> "Weekly"
                                HabitType.CUSTOM_INTERVAL -> "Every ${habit.intervalDays} days"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Streak indicator with best streak
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        StreakIndicator(
                            streakDays = habit.streakDays,
                            showLabel = true,
                            size = StreakSize.Small
                        )
                        if (habit.longestStreak > habit.streakDays) {
                            Text(
                                text = "Best: ${habit.longestStreak}",
                                style = Typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                color = TextTertiary
                            )
                        }
                    }
                }
                
                // Progress to next milestone with gradient
                Spacer(modifier = Modifier.height(Spacing.medium))
                AnimatedProgressBar(
                    progress = progress.coerceIn(0f, 1f),
                    height = 8.dp,
                    showShimmer = progress > 0.8f,
                    gradientColors = when {
                        habit.streakDays >= 100 -> listOf(AccentAmber, Color(0xFFFFD700))
                        habit.streakDays >= 30 -> listOf(GradientStart, GradientEnd)
                        else -> listOf(Success, Primary)
                    }
                )
                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${habit.streakDays}/$nextMilestone days",
                        style = Typography.bodySmall.copy(fontFamily = MonoFontFamily),
                        color = TextSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Next milestone",
                            style = Typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Action buttons with outlined icons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
            ) {
                IconButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = "Edit habit" }
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = { 
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteDialog = true 
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = "Delete habit" }
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitTickDialog(
    result: com.sovereign_rise.app.domain.repository.TickHabitResult,
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
                    imageVector = if (result.milestoneAchieved != null) Icons.Outlined.EmojiEvents else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (result.milestoneAchieved != null) AccentAmber else Success,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = if (result.milestoneAchieved != null) "Milestone Reached!" else "Habit Completed!",
                    style = Typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                StreakIndicator(
                    streakDays = result.newStreakDays,
                    showLabel = true,
                    size = StreakSize.Medium
                )
                
                result.milestoneAchieved?.let { milestone ->
                    Text(
                        text = milestone.message,
                        style = Typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Success
                    )
                }
            }
        },
        confirmButton = {
            GradientButton(
                text = "Awesome!",
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun StreakBreakDialog(
    result: com.sovereign_rise.app.domain.repository.StreakBreakResult,
    onDismiss: () -> Unit
) {
    if (result.brokenHabits.isEmpty()) {
        // No action needed
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Outlined.HeartBroken,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Streak Broken",
                    style = Typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                result.brokenHabits.forEach { habit ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${habit.name} streak reset.",
                            style = Typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
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

@Composable
private fun HabitLockedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Habits Locked",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column {
                Text("Habits will unlock on Tutorial Day 2!")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Complete your Day 1 tutorial tasks to unlock habits and start building daily consistency.",
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

