package com.sovereign_rise.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Extra small - for chips and small badges
    extraSmall = RoundedCornerShape(8.dp),
    
    // Small - for buttons and small cards
    small = RoundedCornerShape(12.dp),
    
    // Medium - for standard cards and dialogs
    medium = RoundedCornerShape(16.dp),
    
    // Large - for bottom sheets and large cards
    large = RoundedCornerShape(20.dp),
    
    // Extra large - for modals and full-screen overlays
    extraLarge = RoundedCornerShape(28.dp)
)

