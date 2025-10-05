package com.sovereign_rise.app.presentation.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sovereign_rise.app.data.local.UsageStatsManager
import com.sovereign_rise.app.presentation.components.*
import com.sovereign_rise.app.presentation.navigation.Screen
import com.sovereign_rise.app.presentation.viewmodel.ProfileUiState
import com.sovereign_rise.app.presentation.viewmodel.ProfileViewModel
import com.sovereign_rise.app.ui.theme.*
import com.sovereign_rise.app.util.observeAsState
import com.sovereign_rise.app.workers.UsageSyncWorker
import kotlinx.coroutines.launch

/**
 * Profile screen displaying user information and account management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showUsagePermissionDialog by remember { mutableStateOf(false) }
    var showDisableTrackingDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Observe connectivity
    val connectivityObserver = remember { com.sovereign_rise.app.di.AppModule.provideConnectivityObserver(context) }
    val connectivityStatus by connectivityObserver.observeAsState()
    val isOnline = connectivityStatus == com.sovereign_rise.app.util.ConnectivityStatus.AVAILABLE
    
    // Usage stats manager and preferences
    val usageStatsManager = remember { UsageStatsManager(context) }
    val trackingPreferences = remember { com.sovereign_rise.app.data.local.UsageTrackingPreferences(context) }
    val isTrackingEnabled by trackingPreferences.isTrackingEnabled.collectAsState(initial = false)
    
    // Refresh profile data when screen resumes
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Show Snackbar for errors
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ProfileUiState.Error -> {
                val result = snackbarHostState.showSnackbar(
                    message = state.message,
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.loadProfile()
                }
            }
            is ProfileUiState.LoggedOut -> {
                // Navigate to login and clear entire backstack
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
            else -> {}
        }
        isRefreshing = false
    }
    
    // Logout confirmation dialog
    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out?",
            confirmText = "Sign Out",
            onConfirm = { viewModel.logout() },
            onDismiss = { showLogoutDialog = false },
            isDangerous = true
        )
    }
    
    // Usage permission dialog
    if (showUsagePermissionDialog) {
        UsageStatsPermissionDialog(
            onDismiss = {
                showUsagePermissionDialog = false
                // Check if permission was granted
                val hasPermission = usageStatsManager.hasUsageStatsPermission()
                if (hasPermission) {
                    // Enable tracking and start syncing
                    scope.launch {
                        trackingPreferences.setTrackingEnabled(true)
                    }
                    UsageSyncWorker.schedule(context)
                    UsageSyncWorker.syncNow(context)
                }
            },
            onPermissionGranted = {
                scope.launch {
                    trackingPreferences.setTrackingEnabled(true)
                }
                UsageSyncWorker.schedule(context)
                UsageSyncWorker.syncNow(context)
            }
        )
    }
    
    // Disable tracking dialog
    if (showDisableTrackingDialog) {
        ConfirmationDialog(
            title = "Disable Usage Tracking",
            message = "This will stop collecting app usage data. To fully revoke permission, you'll need to do this in your phone's Settings > Apps > Sovereign Rise > Permissions.",
            confirmText = "Disable",
            onConfirm = {
                scope.launch {
                    trackingPreferences.setTrackingEnabled(false)
                }
                UsageSyncWorker.cancel(context)
                showDisableTrackingDialog = false
            },
            onDismiss = { showDisableTrackingDialog = false },
            isDangerous = false
        )
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
            is ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            
            is ProfileUiState.Success -> {
                val user = state.user
                val isGuest = state.isGuest
                
                PullToRefreshContainer(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.loadProfile()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.screenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sectionSpacing)
                    ) {
                    // Profile Header
                    item(key = "profile_header") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.default)
                        ) {
                            // Profile Image with gradient border
                            Box(
                                modifier = Modifier
                                    .size(124.dp)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(GradientStart, GradientEnd)
                                        ),
                                        shape = CircleShape
                                    )
                            ) {
                        if (user.profileImageUrl != null) {
                            AsyncImage(
                                model = user.profileImageUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(120.dp)
                                            .clip(CircleShape)
                                            .align(Alignment.Center),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .size(120.dp)
                                            .clip(CircleShape)
                                            .align(Alignment.Center),
                                        color = Primary.copy(alpha = 0.15f)
                            ) {
                                Icon(
                                            imageVector = Icons.Outlined.Person,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .fillMaxSize()
                                                .padding(28.dp),
                                    tint = Primary
                                )
                            }
                        }
                            }
                        
                        Text(
                            text = user.username,
                                style = Typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.semantics { contentDescription = "Username: ${user.username}" }
                        )
                        
                        Text(
                            text = user.email,
                                style = Typography.bodyMedium,
                                color = TextSecondary
                            )
                            
                            // Account type badge with icon
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (isGuest) Warning.copy(alpha = 0.2f) else Success.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isGuest) Icons.Outlined.PersonOutline else Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (isGuest) Warning else Success,
                                    modifier = Modifier.size(16.dp)
                                )
                            Text(
                                text = if (isGuest) "Guest Account" else "Full Account",
                                    style = Typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                color = if (isGuest) Warning else Success
                            )
                        }
                        
                        // Edit Profile Button
                        OutlinedButton(
                            onClick = { navController.navigate(Screen.ProfileEdit.route) },
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                    .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Primary
                            )
                        ) {
                            Icon(
                                    imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                    modifier = Modifier.size(20.dp)
                            )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text("Edit Profile", style = Typography.labelLarge)
                            }
                        }
                    }
                    
                    // Offline banner
                    if (!isOnline) {
                        item(key = "offline_banner") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Warning.copy(alpha = 0.15f)
                                ),
                                shape = MaterialTheme.shapes.medium
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
                                        tint = Warning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "📡 Offline - Some features require internet",
                                        style = Typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                    
                    // Stats Section Header
                    item(key = "stats_header") {
                        Text(
                            text = "Progress",
                            style = Typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Stats - Streak Only
                    item(key = "stats_grid") {
                        ModernStatsCard(
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Outlined.LocalFireDepartment,
                            label = "Streak",
                            value = "${user.streakDays} days",
                            valueColor = if (user.streakDays >= 30) Primary else if (user.streakDays >= 7) Success else Warning,
                            showStreakIndicator = false,
                            streakDays = user.streakDays
                        )
                    }
                    
                    // Account Section Header
                    item(key = "account_header") {
                        Text(
                            text = "Account",
                            style = Typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // AI Features Button
                    item(key = "ai_features_button") {
                        Button(
                            onClick = { 
                                navController.navigate(Screen.AIFeaturesSettings.route)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .semantics { contentDescription = "AI Features Settings" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text("AI Features", style = Typography.labelLarge)
                        }
                    }
                    
                    // Analytics Button
                    item(key = "analytics_button") {
                        Button(
                            onClick = { 
                                if (isTrackingEnabled) {
                                    navController.navigate(Screen.Analytics.route)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Please enable Phone Usage Tracking to view analytics",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .semantics { contentDescription = "Analytics" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTrackingEnabled) Primary else Primary.copy(alpha = 0.5f),
                                contentColor = if (isTrackingEnabled) androidx.compose.ui.graphics.Color.White else TextTertiary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text(
                                text = if (isTrackingEnabled) "Analytics" else "Analytics (Disabled)",
                                style = Typography.labelLarge
                            )
                        }
                    }
                    
                    // Usage Tracking Toggle
                    item(key = "usage_tracking") {
                        UsageTrackingCard(
                            isEnabled = isTrackingEnabled,
                            onClick = {
                                if (isTrackingEnabled) {
                                    // Show disable confirmation dialog
                                    showDisableTrackingDialog = true
                                } else {
                                    // Check if permission is already granted
                                    if (usageStatsManager.hasUsageStatsPermission()) {
                                        // Permission already granted, just enable tracking
                                        scope.launch {
                                            trackingPreferences.setTrackingEnabled(true)
                                        }
                                        UsageSyncWorker.schedule(context)
                                        UsageSyncWorker.syncNow(context)
                                    } else {
                                        // Need to request permission
                                        showUsagePermissionDialog = true
                                    }
                                }
                            }
                        )
                    }
                    
                    // Upgrade Account Button (for guests only)
                    if (isGuest) {
                        item(key = "upgrade_account") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                GradientButton(
                                    text = "Upgrade Account",
                                    onClick = { 
                                        if (isOnline) {
                                            navController.navigate(Screen.UpgradeAccount.route)
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Connect to internet to upgrade your account",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = "Upgrade to full account" },
                                    enabled = isOnline,
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Upgrade,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                )
                                
                                if (!isOnline) {
                                    Text(
                                        text = "Connect to internet to upgrade your account",
                                        style = Typography.bodySmall,
                                        color = TextTertiary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    
                    // Sign Out Button
                    item(key = "sign_out") {
                            OutlinedButton(
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .semantics { contentDescription = "Sign out of account" },
                                colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text("Sign Out", style = Typography.labelLarge)
                        }
                        }
                    }
                }
            }
            
            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.default)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Error,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = state.message,
                            style = Typography.bodyLarge,
                            color = TextPrimary
                        )
                        GradientButton(
                            text = "Retry",
                            onClick = { viewModel.loadProfile() }
                        )
                    }
                }
            }
            
            is ProfileUiState.LoggedOut -> {
                // Navigation handled by LaunchedEffect
            }
        }
        
        // Snackbar host (placed outside when block)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.default)
        )
    }
}

@Composable
private fun ModernStatsCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
    showStreakIndicator: Boolean = false,
    streakDays: Int = 0
) {
    ModernCard(
        modifier = modifier.semantics { contentDescription = "$label: $value" },
        useGlassmorphism = false,
        elevation = Elevation.medium
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(32.dp)
            )
            
            if (showStreakIndicator && streakDays > 0) {
                StreakIndicator(
                    streakDays = streakDays,
                    showLabel = false,
                    size = StreakSize.Small
                )
            } else {
                Text(
                    text = value,
                    style = Typography.titleLarge.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            }
            
            Text(
                text = label,
                style = Typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}
