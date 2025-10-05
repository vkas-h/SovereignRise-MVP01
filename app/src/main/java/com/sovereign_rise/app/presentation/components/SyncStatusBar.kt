package com.sovereign_rise.app.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.sovereign_rise.app.ui.theme.*

/**
 * Sealed class representing overall sync state.
 */
sealed class SyncState {
    object Synced : SyncState()
    data class Syncing(val progress: Int, val total: Int) : SyncState()
    data class Pending(val count: Int) : SyncState()
    data class Failed(val count: Int, val errors: List<String>) : SyncState()
    object Offline : SyncState()
}

/**
 * Global sync status bar component for showing overall sync state.
 */
@Composable
fun SyncStatusBar(
    syncState: SyncState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Show banner based on state (hide for Synced and Pending - auto-sync handles pending)
    val isVisible = when (syncState) {
        is SyncState.Syncing, is SyncState.Failed, is SyncState.Offline -> true
        else -> false
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        when (syncState) {
            is SyncState.Syncing -> {
                SyncingBanner(syncState.progress, syncState.total)
            }
            is SyncState.Failed -> {
                FailedBanner(syncState.count, onRetry)
            }
            is SyncState.Offline -> {
                OfflineBanner()
            }
            else -> {} // Synced or Pending - no banner (auto-sync handles pending)
        }
    }
}

@Composable
private fun SyncingBanner(progress: Int, total: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Info.copy(alpha = 0.2f), Secondary.copy(alpha = 0.2f))
                )
            )
            .border(width = 1.dp, color = BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.default, vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudSync,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Info
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text(
                text = "Syncing $progress/$total changes...",
                style = Typography.labelMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun PendingBanner(count: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Warning.copy(alpha = 0.2f), AccentAmber.copy(alpha = 0.2f))
                )
            )
            .border(width = 1.dp, color = BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.default, vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Warning
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text(
                text = "$count ${if (count == 1) "change" else "changes"} pending",
                style = Typography.labelMedium.copy(fontFamily = MonoFontFamily),
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun FailedBanner(count: Int, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Error.copy(alpha = 0.2f), AccentRose.copy(alpha = 0.2f))
                )
            )
            .border(width = 1.dp, color = BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.default, vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Error
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "$count ${if (count == 1) "change" else "changes"} failed",
                    style = Typography.labelMedium.copy(fontFamily = MonoFontFamily),
                    color = TextPrimary
                )
            }
            
            TextButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Primary
                )
                Spacer(modifier = Modifier.width(Spacing.extraSmall))
                Text(
                    "Retry",
                    style = Typography.labelMedium,
                    color = Primary
                )
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(TextDisabled.copy(alpha = 0.2f), TextTertiary.copy(alpha = 0.2f))
                )
            )
            .border(width = 1.dp, color = BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.default, vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TextTertiary
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text(
                text = "Offline - changes will sync when connected",
                style = Typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}

