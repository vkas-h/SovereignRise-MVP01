package com.sovereign_rise.app.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sovereign_rise.app.domain.model.Affirmation
import com.sovereign_rise.app.domain.model.AffirmationTone
import com.sovereign_rise.app.presentation.components.GradientButton
import com.sovereign_rise.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AffirmationDialog(
    affirmation: Affirmation,
    onDismiss: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }
    val scale by animateFloatAsState(
        targetValue = if (showDialog) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    // Auto-dismiss after 5 seconds
    LaunchedEffect(Unit) {
        delay(5000)
        onDismiss()
    }
    
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            ModernCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .padding(Spacing.default),
                cornerRadius = 24.dp,
                useGlassmorphism = true,
                gradientBorder = true,
                elevation = Elevation.high
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Primary.copy(alpha = 0.1f),
                                    Secondary.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .padding(Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.default)
                ) {
                    // Icon based on tone (Material Icons instead of emoji)
                    Icon(
                        imageVector = when (affirmation.tone) {
                            AffirmationTone.MOTIVATIONAL -> Icons.Outlined.Whatshot
                            AffirmationTone.PHILOSOPHICAL -> Icons.Outlined.Lightbulb
                            AffirmationTone.PRACTICAL -> Icons.Outlined.CheckCircle
                            AffirmationTone.HUMOROUS -> Icons.Outlined.SentimentSatisfied
                        },
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    // Affirmation message
                    Text(
                        text = affirmation.message,
                        style = Typography.headlineSmall,
                        fontFamily = ManropeFontFamily,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = TextPrimary
                    )
                    
                    // Dismiss button
                    GradientButton(
                        text = "Thanks!",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.ThumbUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

