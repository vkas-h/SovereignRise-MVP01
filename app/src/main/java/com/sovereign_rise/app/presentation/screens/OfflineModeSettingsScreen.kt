package com.sovereign_rise.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sovereign_rise.app.presentation.components.SyncState
import com.sovereign_rise.app.presentation.viewmodel.TaskViewModel
import com.sovereign_rise.app.util.Constants
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineModeSettingsScreen(
    taskViewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val syncState by taskViewModel.syncState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Mode & Sync") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sync Status Card
            SyncStatusCard(syncState, taskViewModel)
            
            // Offline Mode Info
            OfflineModeInfoCard()
            
            // Cache Management
            CacheManagementCard()
            
            // Sync Settings
            SyncSettingsCard()
        }
    }
}

@Composable
private fun SyncStatusCard(syncState: SyncState, viewModel: TaskViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (syncState) {
                is SyncState.Synced -> MaterialTheme.colorScheme.primaryContainer
                is SyncState.Syncing -> MaterialTheme.colorScheme.tertiaryContainer
                is SyncState.Pending -> MaterialTheme.colorScheme.secondaryContainer
                is SyncState.Offline -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                is SyncState.Failed -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (syncState) {
                            is SyncState.Synced -> Icons.Default.CheckCircle
                            is SyncState.Syncing -> Icons.Default.Sync
                            is SyncState.Pending -> Icons.Default.CloudQueue
                            is SyncState.Offline -> Icons.Default.CloudOff
                            is SyncState.Failed -> Icons.Default.Error
                        },
                        contentDescription = null,
                        tint = when (syncState) {
                            is SyncState.Synced -> MaterialTheme.colorScheme.primary
                            is SyncState.Syncing -> MaterialTheme.colorScheme.tertiary
                            is SyncState.Pending -> MaterialTheme.colorScheme.secondary
                            is SyncState.Offline -> MaterialTheme.colorScheme.error
                            is SyncState.Failed -> MaterialTheme.colorScheme.error
                        }
                    )
                    
                    Text(
                        text = when (syncState) {
                            is SyncState.Synced -> "All Synced"
                            is SyncState.Syncing -> "Syncing..."
                            is SyncState.Pending -> "Auto-Sync Enabled"
                            is SyncState.Offline -> "Offline Mode"
                            is SyncState.Failed -> "Sync Failed"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Sync details
            when (syncState) {
                is SyncState.Synced -> {
                    Text(
                        "All your data is up to date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                is SyncState.Syncing -> {
                    LinearProgressIndicator(
                        progress = {
                            if (syncState.total > 0) {
                                syncState.progress.toFloat() / syncState.total.toFloat()
                            } else 0f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${syncState.progress} of ${syncState.total} items synced",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is SyncState.Pending -> {
                    Text(
                        "Changes will sync automatically when connected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is SyncState.Offline -> {
                    Text(
                        "Working offline - changes will sync when connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                is SyncState.Failed -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "${syncState.count} ${if (syncState.count == 1) "item" else "items"} failed to sync",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (syncState.errors.isNotEmpty()) {
                            Text(
                                "Last error: ${syncState.errors.first()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.retryFailedSync() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Retry Failed")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineModeInfoCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "How Offline Mode Works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider()
            
            OfflineModeFeature(
                icon = Icons.Default.CloudDone,
                title = "Local-First Storage",
                description = "All your data is stored locally. Works without internet."
            )
            
            OfflineModeFeature(
                icon = Icons.Default.Sync,
                title = "Auto Sync",
                description = "Changes sync automatically when you're back online."
            )
            
            OfflineModeFeature(
                icon = Icons.Default.Security,
                title = "Conflict Resolution",
                description = "Server data wins in conflicts to prevent data loss."
            )
            
            OfflineModeFeature(
                icon = Icons.Default.Schedule,
                title = "Background Sync",
                description = "Syncs every ${Constants.SYNC_CHECK_INTERVAL_MINUTES} minutes when online."
            )
        }
    }
}

@Composable
private fun OfflineModeFeature(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CacheManagementCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Cache Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider()
            
            CacheStat("Cache Duration", "${Constants.OFFLINE_CACHE_DURATION_DAYS} days")
            CacheStat("Max Cache Size", "${Constants.MAX_CACHE_SIZE_MB} MB")
            CacheStat("Max Queued Actions", "${Constants.MAX_QUEUED_ACTIONS}")
            CacheStat("Retry Attempts", "${Constants.SYNC_RETRY_ATTEMPTS}")
        }
    }
}

@Composable
private fun CacheStat(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SyncSettingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Sync Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider()
            
            Text(
                "Sync Priority",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            PriorityItem("Highest", "Task completion, Habit ticks")
            PriorityItem("High", "Updates to tasks and habits")
            PriorityItem("Normal", "Creating new items")
            PriorityItem("Low", "Profile updates")
            
            HorizontalDivider()
            
            Text(
                "Automatic Cleanup",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Cache is automatically cleaned daily to remove old data and failed sync attempts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PriorityItem(priority: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Column {
            Text(
                priority,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

