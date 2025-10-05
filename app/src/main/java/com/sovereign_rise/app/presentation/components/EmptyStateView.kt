package com.sovereign_rise.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sovereign_rise.app.ui.theme.*

/**
 * Modern empty state component with engaging design
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    quote: String? = null,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "empty_state_fade_in"
    )
    
    // Pulsing animation for icon
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    Column(
        modifier = modifier
            .padding(Spacing.extraLarge)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with glow background
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(iconScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.2f),
                                androidx.compose.ui.graphics.Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // Icon with gradient tint
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .scale(iconScale),
                tint = Primary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.large))

        // Title
        Text(
            text = title,
            style = Typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        // Subtitle
        Text(
            text = subtitle,
            style = Typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        // Optional quote
        quote?.let {
            Spacer(modifier = Modifier.height(Spacing.default))
            Text(
                text = "\"$it\"",
                style = Typography.bodyMedium,
                color = TextTertiary,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }

        // Optional action button
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(Spacing.extraLarge))
            GradientButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(0.7f)
            )
        }
    }
}
