package com.sovereign_rise.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sovereign_rise.app.ui.theme.*

@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    backgroundColor: Color = SurfaceVariant,
    gradientColors: List<Color> = listOf(GradientStart, GradientEnd),
    cornerRadius: Dp = 8.dp,
    showShimmer: Boolean = false
) {
    // Animate progress changes
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "progress_animation"
    )
    
    // Shimmer animation for loading states
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val cornerRadiusPx = cornerRadius.toPx()
            
            // Background
            drawRoundRect(
                color = backgroundColor,
                size = Size(canvasWidth, canvasHeight),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
            
            // Progress fill
            if (animatedProgress > 0f) {
                val progressWidth = canvasWidth * animatedProgress
                
                val brush = if (showShimmer) {
                    Brush.linearGradient(
                        colors = listOf(
                            gradientColors[0],
                            gradientColors[1],
                            gradientColors[0]
                        ),
                        start = Offset(progressWidth * shimmerOffset - progressWidth, 0f),
                        end = Offset(progressWidth * shimmerOffset, 0f)
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = gradientColors,
                        startX = 0f,
                        endX = progressWidth
                    )
                }
                
                drawRoundRect(
                    brush = brush,
                    size = Size(progressWidth, canvasHeight),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            }
        }
    }
}

