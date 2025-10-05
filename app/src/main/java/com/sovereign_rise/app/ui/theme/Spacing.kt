package com.sovereign_rise.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing system based on 8dp grid for consistent layouts.
 * Use these values instead of hardcoded dp values throughout the app.
 */
object Spacing {
    val none: Dp = 0.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val default: Dp = 16.dp
    val large: Dp = 24.dp
    val extraLarge: Dp = 32.dp
    val huge: Dp = 40.dp
    val massive: Dp = 48.dp
    
    // Specific use cases
    val cardPadding: Dp = 20.dp
    val screenPadding: Dp = 20.dp
    val sectionSpacing: Dp = 24.dp
    val itemSpacing: Dp = 12.dp
}

/**
 * Elevation system for consistent depth hierarchy.
 */
object Elevation {
    val none: Dp = 0.dp
    val small: Dp = 2.dp
    val low: Dp = 2.dp
    val medium: Dp = 4.dp
    val high: Dp = 8.dp
    val highest: Dp = 16.dp
}

