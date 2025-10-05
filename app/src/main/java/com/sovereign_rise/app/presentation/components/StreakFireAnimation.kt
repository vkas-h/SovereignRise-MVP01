package com.sovereign_rise.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sovereign_rise.app.ui.theme.*

@Composable
fun StreakIndicator(
    streakDays: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    size: StreakSize = StreakSize.Medium
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak_animation")
    
    // Pulsing scale animation - faster for longer streaks
    val animationDuration = when {
        streakDays >= 100 -> 800
        streakDays >= 30 -> 1000
        streakDays >= 7 -> 1200
        else -> 1500
    }
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streak_scale"
    )
    
    // Subtle rotation for high streaks
    val rotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streak_rotation"
    )
    
    // Color based on streak length
    val iconColor = when {
        streakDays >= 100 -> AccentAmber // Gold for 100+ days
        streakDays >= 30 -> Primary // Emerald for 30+ days
        streakDays >= 7 -> Secondary // Blue for 7+ days
        else -> Warning // Orange for < 7 days
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated fire icon with glow
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Glow effect for high streaks
            if (streakDays >= 30) {
                Box(
                    modifier = Modifier
                        .size(size.iconSize + 8.dp)
                        .scale(scale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    iconColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            
            // Fire icon
            Icon(
                imageVector = Icons.Outlined.LocalFireDepartment,
                contentDescription = "Streak",
                tint = iconColor,
                modifier = Modifier
                    .size(size.iconSize)
                    .scale(scale)
                    .rotate(if (streakDays >= 30) rotation else 0f)
            )
        }
        
        // Streak count
        if (showLabel) {
            Text(
                text = "$streakDays",
                style = Typography.titleLarge.copy(
                    fontFamily = MonoFontFamily,
                    fontSize = size.textSize,
                    fontWeight = FontWeight.Bold
                ),
                color = iconColor
            )
            
            Text(
                text = if (streakDays == 1) "day" else "days",
                style = Typography.bodySmall,
                color = TextTertiary,
                fontSize = size.labelSize
            )
        }
    }
}

enum class StreakSize(val iconSize: Dp, val textSize: TextUnit, val labelSize: TextUnit) {
    Small(20.dp, 16.sp, 10.sp),
    Medium(28.dp, 24.sp, 12.sp),
    Large(36.dp, 32.sp, 14.sp)
}

// Backward compatibility - keep old name
@Composable
@Deprecated("Use StreakIndicator instead", ReplaceWith("StreakIndicator(streakDays, modifier)"))
fun StreakFireAnimation(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    StreakIndicator(streakDays = streakDays, modifier = modifier, showLabel = false)
}
