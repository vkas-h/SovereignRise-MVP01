package com.sovereign_rise.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sovereign_rise.app.domain.model.AffirmationTone
import com.sovereign_rise.app.presentation.components.AffirmationDialog
import com.sovereign_rise.app.presentation.viewmodel.AIFeaturesUiState
import com.sovereign_rise.app.presentation.viewmodel.AIFeaturesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIFeaturesSettingsScreen(
    navController: NavController,
    viewModel: AIFeaturesViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val previewAffirmation by viewModel.previewAffirmation.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Features") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AIFeaturesUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AIFeaturesUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Usage Stats Section
                    item {
                        SectionHeader("Usage Tracking")
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text("Enable Usage Stats")
                                    Switch(
                                        checked = state.data.usageStatsEnabled,
                                        onCheckedChange = { viewModel.toggleUsageStats(it) }
                                    )
                                }
                                
                                if (state.data.usageStatsEnabled && !state.data.hasUsageStatsPermission) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { viewModel.requestUsageStatsPermission() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Grant Permission")
                                    }
                                }
                            }
                        }
                    }
                    
                    // Smart Reminders Section
                    item {
                        SectionHeader("Smart Reminders")
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text("Enable Smart Reminders")
                                    Switch(
                                        checked = state.data.smartRemindersEnabled,
                                        onCheckedChange = { viewModel.toggleSmartReminders(it) }
                                    )
                                }
                                Text(
                                    text = "AI suggests optimal reminder times based on your patterns",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Burnout Detection Section
                    item {
                        SectionHeader("Burnout Detection")
                        Card {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text("Enable Burnout Detection")
                                    Switch(
                                        checked = state.data.burnoutDetectionEnabled,
                                        onCheckedChange = { viewModel.toggleBurnoutDetection(it) }
                                    )
                                }
                                
                                if (state.data.isRecoveryModeActive) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Recovery Mode Active", style = MaterialTheme.typography.titleSmall)
                                            Text("50% reduced penalties", style = MaterialTheme.typography.bodySmall)
                                            Button(
                                                onClick = { viewModel.deactivateRecoveryMode() },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Deactivate")
                                            }
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.activateRecoveryMode() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Activate Recovery Mode")
                                    }
                                }
                            }
                        }
                    }
                    
                    // AI Nudges Section
                    item {
                        SectionHeader("AI Nudges")
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text("Enable AI Nudges")
                                    Switch(
                                        checked = state.data.aiNudgesEnabled,
                                        onCheckedChange = { viewModel.toggleAINudges(it) }
                                    )
                                }
                                Text(
                                    text = "Get reminders when spending too much time on distracting apps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Affirmations Section
                    item {
                        SectionHeader("Affirmations")
                        Card {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Receive motivational messages after completing tasks (max 3/day)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Text(
                                    text = "Shown today: ${state.data.affirmationsShownToday}/3",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Tone Selector
                                var toneExpanded by remember { mutableStateOf(false) }
                                
                                Text(
                                    text = "Tone Style",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                ExposedDropdownMenuBox(
                                    expanded = toneExpanded,
                                    onExpandedChange = { toneExpanded = !toneExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = state.data.affirmationTone.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            Icon(Icons.Default.ArrowDropDown, "Select tone")
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        label = { Text("Select Tone") }
                                    )
                                    
                                    ExposedDropdownMenu(
                                        expanded = toneExpanded,
                                        onDismissRequest = { toneExpanded = false }
                                    ) {
                                        AffirmationTone.values().forEach { tone ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Column {
                                                        Text(tone.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                                                        Text(
                                                            text = when (tone) {
                                                                AffirmationTone.MOTIVATIONAL -> "Energetic and action-oriented"
                                                                AffirmationTone.PHILOSOPHICAL -> "Thoughtful and reflective"
                                                                AffirmationTone.PRACTICAL -> "Straightforward and factual"
                                                                AffirmationTone.HUMOROUS -> "Light and playful"
                                                            },
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setAffirmationTone(tone)
                                                    toneExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                // Preview Button
                                Button(
                                    onClick = { viewModel.previewAffirmation(state.data.affirmationTone) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Preview Affirmation")
                                }
                            }
                        }
                    }
                }
            }
            is AIFeaturesUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Error: ${state.message}")
                }
            }
        }
    }
    
    // Show preview affirmation dialog
    previewAffirmation?.let { affirmation ->
        AffirmationDialog(
            affirmation = affirmation,
            onDismiss = { viewModel.clearPreviewAffirmation() }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

