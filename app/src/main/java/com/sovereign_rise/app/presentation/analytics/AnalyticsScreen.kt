package com.sovereign_rise.app.presentation.analytics

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sovereign_rise.app.domain.model.AnalyticsData
import com.sovereign_rise.app.domain.model.AppUsageData
import com.sovereign_rise.app.presentation.components.*
import com.sovereign_rise.app.presentation.viewmodel.AnalyticsUiState
import com.sovereign_rise.app.presentation.viewmodel.AnalyticsViewModel
import com.sovereign_rise.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val context = LocalContext.current
    var isSyncing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(false) }
    
    // Check tracking preferences
    val trackingPreferences = remember { com.sovereign_rise.app.data.local.UsageTrackingPreferences(context) }
    val isTrackingEnabled by trackingPreferences.isTrackingEnabled.collectAsState(initial = false)

    // Check permission on launch
    LaunchedEffect(Unit) {
        val repo = com.sovereign_rise.app.di.AppModule.provideUsageStatsRepository(context)
        hasPermission = repo.hasUsageStatsPermission()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BackgroundPrimary,
                        Primary.copy(alpha = 0.05f),
                        BackgroundSecondary
                    )
                )
            )
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Surface,
                    tonalElevation = Elevation.small
                ) {
                    TopAppBar(
                        title = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Analytics & Insights", 
                                    style = Typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                // Offline indicator chip
                                if (!isOnline) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = TextTertiary.copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CloudOff,
                                                contentDescription = "Offline",
                                                tint = TextTertiary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                "Offline",
                                                style = Typography.labelSmall,
                                                color = TextTertiary
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    Icons.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Surface
                        ),
                        actions = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        isSyncing = true
                                        try {
                                            val repo = com.sovereign_rise.app.di.AppModule.provideUsageStatsRepository(context)
                                            val result = withContext(Dispatchers.IO) {
                                                repo.syncTodayUsageData()
                                            }
                                            isSyncing = false
                                            if (result.isSuccess) {
                                                snackbarHostState.showSnackbar("✓ Synced! ${result.getOrNull()}")
                                                viewModel.refresh()
                                            } else {
                                                snackbarHostState.showSnackbar("✗ Error: ${result.exceptionOrNull()?.message}")
                                            }
                                        } catch (e: Exception) {
                                            isSyncing = false
                                            snackbarHostState.showSnackbar("✗ Error: ${e.message}")
                                        }
                                    }
                                },
                                enabled = !isSyncing && hasPermission && isOnline
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = TextPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = "Sync Now",
                                        tint = if (hasPermission && isOnline) Primary else TextMuted
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.refresh() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                            }
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = BackgroundDark
        ) { padding ->
        if (!isTrackingEnabled) {
            // Show tracking disabled message
            TrackingDisabledContent(
                modifier = Modifier.padding(padding),
                onNavigateBack = { navController.navigateUp() }
            )
        } else if (!hasPermission) {
            // Show permission request UI
            PermissionRequiredContent(
                modifier = Modifier.padding(padding),
                onRequestPermission = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            )
        } else {
        when (val state = uiState) {
            is AnalyticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing your productivity...", color = TextMuted)
                    }
                }
            }
            is AnalyticsUiState.Success -> {
                AnalyticsContent(
                    modifier = Modifier.padding(padding),
                    data = state.data,
                    viewModel = viewModel,
                    isOnline = isOnline
                )
            }
        }
        }
    }
    }
}

@Composable
private fun TrackingDisabledContent(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Warning
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Usage Tracking Disabled",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Phone usage tracking is currently disabled. Please enable it in your Profile settings to view analytics.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Go to Profile")
            }
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Warning
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Usage Access Required",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        Text(
                "To show your screen time and app usage stats, we need permission to access usage data.",
            style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun AnalyticsContent(
    modifier: Modifier = Modifier,
    data: AnalyticsData,
    viewModel: AnalyticsViewModel,
    isOnline: Boolean
) {
    // Ensure we always fetch today's data (period = 1)
    // Remove this LaunchedEffect block - the ViewModel should handle the default period

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Time Card
        item {
            ScreenTimeCard(
                screenTime = data.summary.todayScreenTime
            )
        }

        // Unlocks Card
        item {
            UnlocksCard(
                unlocks = data.summary.todayUnlocks
            )
        }

        // App Usage Stats
        item {
            Text(
                "📱 App Usage Today",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // Show today's app usage only
        if (data.topApps.isNotEmpty()) {
            items(data.topApps.take(10)) { app ->
                AppUsageItem(app)
            }
        } else {
            item {
                EmptyStateCard("No app usage data for today yet")
            }
        }

        // AI Overview Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "🤖 AI Overview",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            AIOverviewCard(data = data, isOnline = isOnline)
        }

        // Footer spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScreenTimeCard(
    screenTime: Int
) {
    val hours = screenTime / 60
    val minutes = screenTime % 60
    
    // Add validation
    val displayHours = hours.coerceIn(0, 24)
    val displayMinutes = minutes.coerceIn(0, 59)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Screen Time",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Today", // Emphasize this is TODAY only
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (displayHours > 0) {
                    Text(
                        "${displayHours}h",
                        style = MaterialTheme.typography.displaySmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "${displayMinutes}m",
                    style = MaterialTheme.typography.displaySmall,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun UnlocksCard(
    unlocks: Int
) {
    // Validate unlock count
    val displayUnlocks = unlocks.coerceIn(0, 500)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Unlocks",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Today",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Text(
                "$displayUnlocks",
                style = MaterialTheme.typography.displaySmall,
                color = Accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AppUsageItem(app: AppUsageData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (app.isProductive) Success else Danger)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                Text(
                        text = if (app.isProductive) "Productive" else "Distracting",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val hours = (app.totalMinutes / 60).coerceIn(0, 24)
                val minutes = (app.totalMinutes % 60).coerceIn(0, 59)
                Text(
                    text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (app.isProductive) Success else Danger,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AIOverviewCard(
    data: AnalyticsData,
    isOnline: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // If offline or no AI insights, show connect message
        if (!isOnline || !data.hasAIInsights) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "AI Overview Unavailable",
                    tint = TextTertiary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "AI Overview Unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Connect to internet to get AI-powered insights about your productivity patterns.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return@Card
        }
        
        // If online and has AI insights, show the insights
        Column(modifier = Modifier.padding(20.dp)) {
            // Task Completion
            AIOverviewSection(
                icon = Icons.Default.CheckCircle,
                title = "Task Completion",
                content = generateTaskCompletionText(data),
                iconColor = Success
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = TextMuted.copy(alpha = 0.2f)
            )

            // Distracting Apps
            AIOverviewSection(
                icon = Icons.Default.PhoneAndroid,
                title = "What Apps Are Distracting Me",
                content = generateDistractingAppsText(data),
                iconColor = Warning
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = TextMuted.copy(alpha = 0.2f)
            )

            // What Might Be My Fault
            AIOverviewSection(
                icon = Icons.Default.Warning,
                title = "What Might Be My Fault",
                content = generateFaultAnalysisText(data),
                iconColor = Danger
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = TextMuted.copy(alpha = 0.2f)
            )

            // How to Fix Habits
            AIOverviewSection(
                icon = Icons.Default.Psychology,
                title = "How to Fix My Habits & Be Better",
                content = generateHabitFixText(data),
                iconColor = Info
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = TextMuted.copy(alpha = 0.2f)
            )

            // Personalized Challenge
            AIOverviewSection(
                icon = Icons.Default.EmojiEvents,
                title = "Personalized Challenge",
                content = generatePersonalizedChallenge(data),
                iconColor = Primary
            )
        }
    }
}

@Composable
private fun AIOverviewSection(
    icon: ImageVector,
    title: String,
    content: String,
    iconColor: Color
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
        )
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
                    Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// Helper functions to generate AI insights text
private fun generateTaskCompletionText(data: AnalyticsData): String {
    // Get today's task data - using lastOrNull() because taskCompletionRate contains today's data
    // (either single entry from local analytics or last entry from backend ordered by date ASC)
    val todayTasks = data.taskCompletionRate.lastOrNull()
    val todayCompleted = todayTasks?.tasksCompleted ?: 0
    val todayTotal = todayTasks?.let { it.tasksCompleted + it.tasksPending + it.tasksFailed } ?: 0
    
    if (todayTotal == 0) {
        return "No tasks scheduled for today. Consider planning your day to stay productive!"
    }
    
    val todayRate = if (todayTotal > 0) (todayCompleted.toDouble() / todayTotal * 100).toInt() else 0
    return when {
        todayRate >= 80 -> "Excellent! You've completed $todayCompleted out of $todayTotal tasks today ($todayRate%). Keep up this amazing momentum!"
        todayRate >= 50 -> "Good progress! You've completed $todayCompleted out of $todayTotal tasks ($todayRate%). You're on track!"
        todayRate > 0 -> "You've completed $todayCompleted out of $todayTotal tasks ($todayRate%). Focus on finishing your remaining tasks."
        else -> "You have $todayTotal tasks pending. Time to get started and build that momentum!"
    }
}

private fun generateDistractingAppsText(data: AnalyticsData): String {
    val distractingApps = data.topApps.filter { !it.isProductive }.take(3)
    
    if (distractingApps.isEmpty()) {
        return "Great news! You're spending most of your time on productive apps. Keep it up!"
    }
    
    val topDistractor = distractingApps.first()
    val totalDistractingTime = distractingApps.sumOf { it.totalMinutes }
    
    val appList = distractingApps.joinToString(", ") { "${it.appName} (${it.totalMinutes / 60}h ${it.totalMinutes % 60}m)" }
    
    return "Your top distractors are: $appList. That's ${totalDistractingTime / 60}h ${totalDistractingTime % 60}m total. Consider setting time limits on ${topDistractor.appName} to reclaim your focus."
}

private fun generateFaultAnalysisText(data: AnalyticsData): String {
    val issues = mutableListOf<String>()
    
    // Check today's screen time
    val screenTime = data.summary.todayScreenTime
    
    if (screenTime > 180) { // 3+ hours
        issues.add("excessive screen time today (${screenTime / 60}h ${screenTime % 60}m)")
    }
    
    // Check today's unlocks
    val unlocks = data.summary.todayUnlocks
    
    if (unlocks > 100) {
        issues.add("frequent phone checking ($unlocks unlocks today)")
    }
    
    // Check task completion - using today's data (last entry in the list)
    val todayTasks = data.taskCompletionRate.lastOrNull()
    val todayCompleted = todayTasks?.tasksCompleted ?: 0
    val todayTotal = todayTasks?.let { it.tasksCompleted + it.tasksPending + it.tasksFailed } ?: 0
    val todayRate = if (todayTotal > 0) todayCompleted.toDouble() / todayTotal else 0.0
    
    if (todayRate < 0.5 && todayTotal > 0) {
        issues.add("low task completion rate today (${(todayRate * 100).toInt()}%)")
    }
    
    // Check distracting time
    val distractingApps = data.topApps.filter { !it.isProductive }
    val distractingTime = distractingApps.sumOf { it.totalMinutes }
    val productiveTime = data.topApps.filter { it.isProductive }.sumOf { it.totalMinutes }
    val totalAppTime = distractingTime + productiveTime
    
    val distractingPercent = if (totalAppTime > 0) {
        (distractingTime.toDouble() / totalAppTime * 100).toInt()
    } else 0
    
    if (distractingPercent > 60) {
        issues.add("$distractingPercent% of time on distracting apps")
    }
    
    return if (issues.isEmpty()) {
        "You're doing great today! No major issues detected. Keep maintaining your healthy habits and productivity patterns."
    } else {
        "Areas to work on: ${issues.joinToString("; ")}. Awareness is the first step to improvement!"
    }
}

private fun generateHabitFixText(data: AnalyticsData): String {
    val recommendations = mutableListOf<String>()
    
    // Screen time recommendations
    val screenTime = data.summary.todayScreenTime
    
    if (screenTime > 180) {
        recommendations.add("Set a daily screen time limit of 2-3 hours")
    }
    
    // Unlock recommendations
    val unlocks = data.summary.todayUnlocks
    
    if (unlocks > 100) {
        recommendations.add("Reduce phone checks by turning off non-essential notifications")
    }
    
    // Task completion recommendations - using today's data (last entry in the list)
    val todayTasks = data.taskCompletionRate.lastOrNull()
    val todayCompleted = todayTasks?.tasksCompleted ?: 0
    val todayTotal = todayTasks?.let { it.tasksCompleted + it.tasksPending + it.tasksFailed } ?: 0
    val todayRate = if (todayTotal > 0) todayCompleted.toDouble() / todayTotal else 0.0
    
    if (todayRate < 0.7 && todayTotal > 0) {
        recommendations.add("Break tasks into smaller, manageable chunks")
        recommendations.add("Use time-blocking to dedicate focused periods to tasks")
    }
    
    // Distracting apps recommendations
    val distractingApps = data.topApps.filter { !it.isProductive }.take(2)
    if (distractingApps.isNotEmpty()) {
        recommendations.add("Use app timers for ${distractingApps.first().appName}")
    }
    
    // General recommendations
    if (data.summary.currentStreak < 3) {
        recommendations.add("Build consistency by completing at least one task daily")
    }
    
    return if (recommendations.isEmpty()) {
        "You're on the right track! Continue your current habits: staying focused, completing tasks, and maintaining a healthy phone usage balance."
    } else {
        recommendations.take(4).joinToString("\n• ", "• ")
    }
}

private fun generatePersonalizedChallenge(data: AnalyticsData): String {
    // Generate personalized challenges based on today's performance
    
    // Screen time challenge
    val screenTime = data.summary.todayScreenTime
    
    if (screenTime > 180) {
        val targetTime = (screenTime * 0.8).toInt()
        return "🎯 Screen Time Challenge: Reduce your screen time tomorrow to ${targetTime / 60}h ${targetTime % 60}m (20% reduction). You can do this!"
    }
    
    // Task completion challenge - using today's data (last entry in the list)
    val todayTasks = data.taskCompletionRate.lastOrNull()
    val todayCompleted = todayTasks?.tasksCompleted ?: 0
    val todayTotal = todayTasks?.let { it.tasksCompleted + it.tasksPending + it.tasksFailed } ?: 0
    val todayRate = if (todayTotal > 0) todayCompleted.toDouble() / todayTotal else 0.0
    
    if (todayRate < 0.7 && todayTotal > 0) {
        return "🎯 Task Master Challenge: Achieve 100% task completion tomorrow. Start by completing all tasks for 3 consecutive days!"
    }
    
    // Unlock challenge
    val unlocks = data.summary.todayUnlocks
    
    if (unlocks > 100) {
        val targetUnlocks = (unlocks * 0.7).toInt()
        return "🎯 Mindful Usage Challenge: Reduce phone unlocks to under $targetUnlocks tomorrow. Be more intentional with every unlock!"
    }
    
    // Streak challenge
    if (data.summary.currentStreak < 7) {
        return "🎯 Consistency Challenge: Build a 7-day streak! Complete at least one task every day to build unstoppable momentum."
    }
    
    // Productive vs distracting time challenge
    val distractingApps = data.topApps.filter { !it.isProductive }
    val distractingTime = distractingApps.sumOf { it.totalMinutes }
    val productiveTime = data.topApps.filter { it.isProductive }.sumOf { it.totalMinutes }
    val totalAppTime = distractingTime + productiveTime
    
    val productivePercent = if (totalAppTime > 0) {
        (productiveTime.toDouble() / totalAppTime * 100).toInt()
    } else 0
    
    if (productivePercent < 50) {
        return "🎯 Focus Challenge: Flip the ratio tomorrow! Spend more time on productive apps than distracting ones. Start with one focused hour."
    }
    
    // Default challenge for high performers
    return "🎯 Excellence Challenge: You're doing great! Push for 100% task completion and under 2 hours screen time tomorrow. Become the best version of yourself!"
}
