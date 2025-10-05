package com.sovereign_rise.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sovereign_rise.app.domain.model.BurnoutAlert
import com.sovereign_rise.app.domain.model.BurnoutLevel
import com.sovereign_rise.app.ui.theme.*

@Composable
fun BurnoutAlertDialog(
    alert: BurnoutAlert,
    onActivateRecoveryMode: () -> Unit,
    onReduceTasks: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.SelfImprovement,
                contentDescription = "Burnout Alert",
                tint = when (alert.level) {
                    BurnoutLevel.MODERATE -> Warning
                    BurnoutLevel.SEVERE -> Error
                    else -> TextSecondary
                },
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = when (alert.level) {
                    BurnoutLevel.MODERATE -> "Taking Care of Yourself"
                    BurnoutLevel.SEVERE -> "Let's Adjust Your Goals"
                    else -> "Wellness Check"
                },
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "We noticed you might be feeling overwhelmed. It's okay to adjust your pace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Metrics
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Current Metrics:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Completion rate: ${(alert.metrics.completionRate * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Missed tasks: ${alert.metrics.missedTaskCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (alert.metrics.lateNightActivityMinutes > 0) {
                            Text(
                                text = "• Late-night activity: ${alert.metrics.lateNightActivityMinutes} min",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                // Recommendations
                if (alert.recommendations.isNotEmpty()) {
                    Text(
                        text = "Recommendations:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    alert.recommendations.forEach { recommendation ->
                        Text(
                            text = "• $recommendation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onActivateRecoveryMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Activate Recovery Mode")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReduceTasks) {
                    Text("Reduce Tasks")
                }
                TextButton(onClick = onDismiss) {
                    Text("I'm OK")
                }
            }
        }
    )
}

