package com.sovereign_rise.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sovereign_rise.app.presentation.navigation.Screen
import com.sovereign_rise.app.ui.theme.*

/**
 * Navigation item data class with filled and outlined icon variants
 */
private data class NavigationItem(
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
    val label: String,
    val route: String
)

/**
 * Modern bottom navigation bar with sleek design and smooth animations
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?
) {
    // Track selected route locally for instant UI updates
    var selectedRoute by remember { mutableStateOf(currentRoute) }
    
    // Update selected route when actual route changes
    LaunchedEffect(currentRoute) {
        selectedRoute = currentRoute
    }
    
    val navigationItems = listOf(
        NavigationItem(
            iconOutlined = Icons.Outlined.Home,
            iconFilled = Icons.Filled.Home,
            label = "Home",
            route = Screen.Home.route
        ),
        NavigationItem(
            iconOutlined = Icons.Outlined.TaskAlt,
            iconFilled = Icons.Filled.TaskAlt,
            label = "Tasks",
            route = Screen.Tasks.route
        ),
        NavigationItem(
            iconOutlined = Icons.Outlined.Loop,
            iconFilled = Icons.Filled.Loop,
            label = "Habits",
            route = Screen.Habits.route
        ),
        NavigationItem(
            iconOutlined = Icons.Outlined.BarChart,
            iconFilled = Icons.Filled.BarChart,
            label = "Analytics",
            route = Screen.Analytics.route
        ),
        NavigationItem(
            iconOutlined = Icons.Outlined.Person,
            iconFilled = Icons.Filled.Person,
            label = "Profile",
            route = Screen.Profile.route
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = BackgroundSecondary,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            OverlayDark
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = BorderSubtle,
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                navigationItems.forEach { item ->
                    NavigationItem(
                        item = item,
                        isSelected = selectedRoute == item.route,
                        onClick = {
                            // Only navigate if not already on this route
                            if (selectedRoute != item.route) {
                                // Update UI instantly
                                selectedRoute = item.route
                                // Then navigate
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationItem(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // Instant background color - no animation
    val backgroundColor = if (isSelected) {
        Primary.copy(alpha = 0.15f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }
    
    // Instant icon color - no animation
    val iconColor = if (isSelected) Primary else TextTertiary
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(Spacing.small)
    ) {
        Icon(
            imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Extension function to get current route from NavController
 */
fun NavController.getCurrentRoute(): String? {
    return currentBackStackEntry?.destination?.route
}
