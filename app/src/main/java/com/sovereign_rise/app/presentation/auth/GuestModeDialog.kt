package com.sovereign_rise.app.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sovereign_rise.app.presentation.components.GradientButton
import com.sovereign_rise.app.ui.theme.*

/**
 * Dialog shown after guest login to inform users about limitations and upgrade options.
 */
@Composable
fun GuestModeDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.PersonOutline,
                contentDescription = "Guest Mode",
                tint = Warning,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Guest Mode",
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.default)) {
                Text(
                    text = "You're using Sovereign Rise as a guest",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "Guest limitations:",
                        style = Typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Cancel, null, modifier = Modifier.size(16.dp), tint = Error)
                        Text("No leaderboards", style = Typography.bodySmall, color = TextSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Cancel, null, modifier = Modifier.size(16.dp), tint = Error)
                        Text("No guilds", style = Typography.bodySmall, color = TextSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Cancel, null, modifier = Modifier.size(16.dp), tint = Error)
                        Text("No social features", style = Typography.bodySmall, color = TextSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp), tint = Success)
                        Text("All progress saved locally", style = Typography.bodySmall, color = Success)
                    }
                }
                
                Text(
                    text = "Upgrade to a full account to unlock all features and sync across devices.",
                    style = Typography.bodySmall,
                    color = TextTertiary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Outlined.Upgrade, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.extraSmall))
                Text("Upgrade Now", style = Typography.labelLarge)
            }
        },
        dismissButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                TextButton(onClick = onContinue) {
                    Text("Continue as Guest", style = Typography.labelMedium, color = TextSecondary)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", style = Typography.labelMedium)
                }
            }
        }
    )
}
