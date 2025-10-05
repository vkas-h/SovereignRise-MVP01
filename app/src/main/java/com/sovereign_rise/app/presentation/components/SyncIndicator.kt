package com.sovereign_rise.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.sovereign_rise.app.ui.theme.*

/**
 * Modern sync indicator component with outlined icons and smooth animations.
 */
@Composable
fun SyncIndicator(
    syncState: SyncState,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit = {}
) {
    // Icon with optional animation
    when (syncState) {
        is SyncState.Synced -> {
            SyncIcon(
                icon = Icons.Outlined.CloudDone,
                tint = Success,
                contentDescription = "Synced",
                modifier = modifier,
                compact = compact,
                onClick = onClick
            )
        }
        is SyncState.Pending -> {
            // Auto-sync handles pending - treat as synced
            SyncIcon(
                icon = Icons.Outlined.CloudDone,
                tint = Success,
                contentDescription = "Auto-syncing",
                modifier = modifier,
                compact = compact,
                onClick = onClick
            )
        }
        is SyncState.Syncing -> {
            // Continuous rotation for syncing
            val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )
            
            SyncIcon(
                icon = Icons.Outlined.CloudSync,
                tint = Primary,
                contentDescription = "Syncing: ${syncState.progress} of ${syncState.total}",
                modifier = modifier.rotate(rotation),
                compact = compact,
                onClick = onClick
            )
        }
        is SyncState.Offline -> {
            SyncIcon(
                icon = Icons.Outlined.CloudOff,
                tint = Error,
                contentDescription = "Offline mode",
                modifier = modifier,
                compact = compact,
                onClick = onClick
            )
        }
        is SyncState.Failed -> {
            SyncIcon(
                icon = Icons.Outlined.CloudOff,
                tint = Error,
                contentDescription = "Sync failed: ${syncState.count} items",
                modifier = modifier,
                compact = compact,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun SyncIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit = {}
) {
    if (compact) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier.size(16.dp)
        )
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}
