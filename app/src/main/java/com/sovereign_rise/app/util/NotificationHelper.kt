package com.sovereign_rise.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sovereign_rise.app.MainActivity
import com.sovereign_rise.app.R
import com.sovereign_rise.app.domain.model.BurnoutAlert
import com.sovereign_rise.app.domain.model.BurnoutLevel

object NotificationHelper {
    
    // Notification IDs
    const val NOTIFICATION_ID_SMART_REMINDER = 1001
    const val NOTIFICATION_ID_USAGE_NUDGE = 1002
    const val NOTIFICATION_ID_AFFIRMATION = 1003
    const val NOTIFICATION_ID_BURNOUT_ALERT = 1004
    const val NOTIFICATION_ID_TASK_REMINDER_BASE = 2000
    const val NOTIFICATION_ID_HABIT_REMINDER_BASE = 3000
    
    /**
     * Create all notification channels (Android 8.0+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Smart Reminders channel
            val remindersChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_REMINDERS,
                "Smart Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AI-suggested task reminders"
                enableVibration(true)
            }
            
            // AI Nudges channel
            val nudgesChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_NUDGES,
                "AI Nudges",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Usage-based productivity nudges"
                enableVibration(true)
            }
            
            // Affirmations channel
            val affirmationsChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_AFFIRMATIONS,
                "Affirmations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Motivational messages"
                enableVibration(false)
            }
            
            // Burnout Alerts channel
            val burnoutChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_BURNOUT,
                "Burnout Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wellness and burnout warnings"
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannels(listOf(
                remindersChannel,
                nudgesChannel,
                affirmationsChannel,
                burnoutChannel
            ))
        }
    }
    
    /**
     * Show a usage nudge notification
     * 
     * Note: For truly dynamic messages, call backend endpoint /api/ai/nudges
     * to get AI-generated nudge text based on user context
     */
    fun showUsageNudge(context: Context, appName: String, usageTime: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "tasks")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_NUDGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with actual icon
            .setContentTitle("Time to refocus")
            .setContentText("You've been on $appName for $usageTime+ min. Redirect that energy into a task!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                0,
                "View Tasks",
                pendingIntent
            )
            .build()
        
        showNotification(context, NOTIFICATION_ID_USAGE_NUDGE, notification)
    }
    
    /**
     * Show a burnout alert notification
     */
    fun showBurnoutAlert(context: Context, alert: BurnoutAlert, autoRecovery: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "ai_features")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = when (alert.level) {
            BurnoutLevel.MODERATE -> "Taking Care of Yourself"
            BurnoutLevel.SEVERE -> "Let's Adjust Your Goals"
            else -> "Wellness Check"
        }
        
        val message = if (autoRecovery) {
            "Recovery Mode activated. 50% reduced penalties for 3 days. Take it easy."
        } else {
            "We noticed you might be feeling overwhelmed. Consider activating Recovery Mode."
        }
        
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_BURNOUT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        showNotification(context, NOTIFICATION_ID_BURNOUT_ALERT, notification)
    }
    
    /**
     * Show an affirmation notification
     */
    fun showAffirmation(context: Context, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_AFFIRMATIONS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("You're doing great!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        showNotification(context, NOTIFICATION_ID_AFFIRMATION, notification)
    }
    
    /**
     * Show a smart reminder notification
     */
    fun showSmartReminder(context: Context, taskTitle: String, suggestedTime: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "tasks")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Smart Reminder")
            .setContentText("$taskTitle - Suggested time: $suggestedTime")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        showNotification(context, NOTIFICATION_ID_SMART_REMINDER, notification)
    }
    
    /**
     * Show a task reminder notification
     */
    fun showTaskReminder(context: Context, taskId: String, taskTitle: String, taskDescription: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "tasks")
            putExtra("task_id", taskId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Here's your task reminder")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        
        // Add description if available
        if (!taskDescription.isNullOrBlank()) {
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$taskTitle\n\n$taskDescription")
            )
        }
        
        val notification = notificationBuilder.build()
        
        showNotification(context, NOTIFICATION_ID_TASK_REMINDER_BASE + taskId.hashCode(), notification)
    }
    
    /**
     * Show a habit reminder notification
     */
    fun showHabitReminder(context: Context, habitId: String, habitName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "habits")
            putExtra("habit_id", habitId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Habit Reminder")
            .setContentText("Time to complete: $habitName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        showNotification(context, NOTIFICATION_ID_HABIT_REMINDER_BASE + habitId.hashCode(), notification)
    }
    
    /**
     * Show a notification
     */
    private fun showNotification(context: Context, notificationId: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check for notification permission on Android 13+
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

