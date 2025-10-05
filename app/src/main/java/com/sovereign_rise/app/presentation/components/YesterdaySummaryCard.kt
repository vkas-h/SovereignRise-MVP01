package com.sovereign_rise.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sovereign_rise.app.domain.model.DailyTaskSummary
import com.sovereign_rise.app.domain.model.TaskStatus
import com.sovereign_rise.app.ui.theme.*

/**
 * Card displaying yesterday's task summary.
 */
@Composable
fun YesterdaySummaryCard(
    summary: DailyTaskSummary,
    modifier: Modifier = Modifier
) {
    val hasData = summary.hasSummary && summary.totalTasks > 0
    
    ModernCard(
        modifier = modifier.fillMaxWidth(),
        useGlassmorphism = hasData && summary.completionRate >= 80,
        gradientBorder = hasData && summary.completionRate == 100f,
        elevation = Elevation.medium
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Yesterday's Performance",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (hasData) summary.dateString else "No data available",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                
                // Completion Rate Badge (only show if has data)
                if (hasData) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    summary.completionRate >= 80 -> SuccessLight
                                    summary.completionRate >= 50 -> WarningLight
                                    else -> ErrorLight
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${summary.completionRate.toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                summary.completionRate >= 80 -> Success
                                summary.completionRate >= 50 -> Warning
                                else -> Error
                            }
                        )
                    }
                }
            }
            
            if (hasData) {
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Total Tasks
                    StatItem(
                        label = "Total",
                        value = summary.totalTasks.toString(),
                        color = Primary
                    )
                    
                    // Completed
                    StatItem(
                        label = "Completed",
                        value = summary.completedTasks.toString(),
                        color = Success,
                        icon = Icons.Outlined.CheckCircle
                    )
                    
                    // Missed
                    StatItem(
                        label = "Missed",
                        value = summary.failedTasks.toString(),
                        color = Error,
                        icon = Icons.Outlined.Cancel
                    )
                }
                
                // Task List (horizontal scroll)
                if (summary.tasks.isNotEmpty()) {
                    Text(
                        text = "Tasks:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(summary.tasks.take(10)) { task ->
                            TaskChip(
                                title = task.title,
                                status = task.status,
                                difficulty = task.difficulty.name
                            )
                        }
                        
                        if (summary.tasks.size > 10) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${summary.tasks.size - 10} more",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty state when no data
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EventBusy,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No tasks from yesterday",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Complete some tasks today!",
                        style = Typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun TaskChip(
    title: String,
    status: TaskStatus,
    @Suppress("UNUSED_PARAMETER") difficulty: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        TaskStatus.COMPLETED -> SuccessLight
        TaskStatus.FAILED -> ErrorLight
        else -> SurfaceVariant
    }
    
    val textColor = when (status) {
        TaskStatus.COMPLETED -> Success
        TaskStatus.FAILED -> Error
        else -> TextSecondary
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(textColor)
            )
            
            Text(
                text = title,
                fontSize = 12.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
        }
    }
}

