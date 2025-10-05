package com.sovereign_rise.app.presentation.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sovereign_rise.app.ui.theme.*

/**
 * Dialog explaining and requesting usage stats permission
 */
@Composable
fun UsageStatsPermissionDialog(
    onDismiss: () -> Unit,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Primary
            )
        },
        title = {
            Text(
                "Enable Phone Usage Tracking",
                style = Typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Get detailed insights into your phone usage patterns and productivity!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "📊 What you'll get:",
                    style = MaterialTheme.typography.titleSmall,
                    color = Primary,
                    modifier = Modifier.fillMaxWidth()
                )
                
                BulletPoint("Track screen time and app usage")
                BulletPoint("See productive vs distracting app breakdown")
                BulletPoint("Get AI-powered insights on your habits")
                BulletPoint("Identify your peak productivity hours")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "🔒 Your privacy:",
                    style = MaterialTheme.typography.titleSmall,
                    color = Success,
                    modifier = Modifier.fillMaxWidth()
                )
                
                BulletPoint("Data is only used for your analytics")
                BulletPoint("No personal information is collected")
                BulletPoint("You can disable this anytime")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "To enable, you'll be taken to Android settings to grant \"Usage Access\" permission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Open usage stats settings
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Enable Tracking")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later", color = TextMuted)
            }
        },
        containerColor = SurfaceDark,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "•",
            style = MaterialTheme.typography.bodyMedium,
            color = Primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Card component to show in settings or profile to enable usage tracking
 */
@Composable
fun UsageTrackingCard(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) Success.copy(alpha = 0.1f) else SurfaceDark
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = if (isEnabled) Success else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Phone Usage Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isEnabled) "Enabled - Tracking your app usage" 
                    else "Enable to get detailed analytics",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled) Success else TextMuted
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = { onClick() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Success,
                    checkedTrackColor = Success.copy(alpha = 0.5f)
                )
            )
        }
    }
}

