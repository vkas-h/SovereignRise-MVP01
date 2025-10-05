package com.sovereign_rise.app.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sovereign_rise.app.presentation.components.*
import com.sovereign_rise.app.presentation.navigation.Screen
import com.sovereign_rise.app.presentation.viewmodel.HomeViewModel
import com.sovereign_rise.app.presentation.viewmodel.HomeUiState
import com.sovereign_rise.app.presentation.viewmodel.TaskViewModel
import com.sovereign_rise.app.ui.theme.*
import com.sovereign_rise.app.util.observeAsState
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Home screen displaying user progression, stats, and quick actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    taskViewModel: TaskViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncState by taskViewModel.syncState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Observe connectivity
    val connectivityObserver = remember { com.sovereign_rise.app.di.AppModule.provideConnectivityObserver(context) }
    val connectivityStatus by connectivityObserver.observeAsState()
    val isOnline = connectivityStatus == com.sovereign_rise.app.util.ConnectivityStatus.AVAILABLE
    
    // Check if user is offline guest
    val authRepository = remember { com.sovereign_rise.app.di.AppModule.provideAuthRepository(context) }
    var isOfflineGuest by remember { mutableStateOf(false) }
    
    // Get current nav back stack entry to detect when returning to this screen
    val currentBackStackEntry = navController.currentBackStackEntry
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    // Refresh data when screen becomes visible or when returning to it
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadHomeData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(Unit) {
        isOfflineGuest = authRepository.isOfflineGuest()
    }
    
    // Show Snackbar for errors
    LaunchedEffect(uiState) {
        if (uiState is HomeUiState.Error) {
            val result = snackbarHostState.showSnackbar(
                message = (uiState as HomeUiState.Error).message,
                actionLabel = "Retry",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.resetState()
                viewModel.loadHomeData()
            }
        }
        isRefreshing = false
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundPrimary, BackgroundSecondary)
                )
            )
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            
            is HomeUiState.Success -> {
                PullToRefreshContainer(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.loadHomeData()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sectionSpacing)
                    ) {
                        // Custom Header with Greeting
                        item(key = "header") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = getGreeting(),
                                            style = Typography.headlineMedium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Ready to rise?",
                                            style = Typography.bodyLarge,
                                            color = TextSecondary
                                        )
                                    }
                                    
                                    // Right side: Profile icon
                                    // Profile icon (clickable to navigate to profile)
                                    IconButton(
                                        onClick = { navController.navigate(Screen.Profile.route) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AccountCircle,
                                            contentDescription = "Profile",
                                            tint = Primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Offline banner
                        if (!isOnline) {
                            item(key = "offline_banner") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isOfflineGuest) Info.copy(alpha = 0.15f) else Warning.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CloudOff,
                                            contentDescription = "Offline",
                                            tint = if (isOfflineGuest) Info else Warning,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (isOfflineGuest) {
                                                "📡 Offline Mode - Data will sync when you connect to internet"
                                            } else {
                                                "📡 Offline - Changes will sync when connected"
                                            },
                                            style = Typography.bodySmall,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Yesterday's Summary (always show, with empty state if no data)
                        state.data.yesterdaySummary?.let { summary ->
                            item(key = "yesterday_summary") {
                                YesterdaySummaryCard(summary = summary)
                            }
                        }
                        
                        // Quick Stats Grid
                        item(key = "stats_header") {
                            Text(
                                text = "Today's Progress",
                                style = Typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        
                        item(key = "stats_grid") {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.itemSpacing)
                                ) {
                                    ModernStatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Outlined.TaskAlt,
                                        title = "Tasks",
                                        value = "${state.data.tasksCompletedToday} / ${state.data.tasksCompletedToday + state.data.tasksPendingToday}",
                                        color = Success,
                                        onClick = { navController.navigate(Screen.Tasks.route) }
                                    )
                                    
                                    ModernStatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Outlined.Loop,
                                        title = "Habits",
                                        value = "${state.data.habitsCheckedToday} / ${state.data.habitsCheckedToday + state.data.habitsPendingToday}",
                                        color = Info,
                                        onClick = { navController.navigate(Screen.Habits.route) }
                                    )
                                }
                                
                                ModernStatCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Outlined.LocalFireDepartment,
                                    title = "Streak",
                                    value = "${state.data.currentStreak} days",
                                    color = Warning,
                                    onClick = {},
                                    showStreakIndicator = true,
                                    streakDays = state.data.currentStreak
                                )
                            }
                        }
                        
                        // Quick Actions
                        item(key = "actions_header") {
                            Text(
                                text = "Quick Actions",
                                style = Typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        
                        item(key = "add_task_button") {
                            GradientButton(
                                text = "Add New Task",
                                onClick = { navController.navigate(Screen.AddTask.route) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Add New Task" },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.default)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = state.message,
                            style = Typography.bodyLarge,
                            color = Error
                        )
                        GradientButton(
                            text = "Retry",
                            onClick = { viewModel.resetState() },
                            gradientColors = listOf(Error, AccentRose)
                        )
                    }
                }
            }
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.default)
        )
    }
}

@Composable
private fun ModernStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    onClick: () -> Unit,
    showStreakIndicator: Boolean = false,
    streakDays: Int = 0
) {
    // Animate value count-up
    val animatedValue by remember(value) {
        mutableStateOf(value)
    }
    
    ModernCard(
        modifier = modifier.semantics { contentDescription = "$title: $value" },
        onClick = onClick,
        elevation = Elevation.medium,
        useGlassmorphism = showStreakIndicator && streakDays >= 30
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            if (showStreakIndicator && streakDays > 0) {
                StreakIndicator(
                    streakDays = streakDays,
                    size = StreakSize.Medium,
                    showLabel = false
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Text(
                text = title,
                style = Typography.bodyMedium,
                color = TextTertiary
            )
            
            Text(
                text = animatedValue,
                style = Typography.titleMedium.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

private fun getGreeting(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}
