package com.sovereign_rise.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sovereign_rise.app.receiver.ReminderBroadcastReceiver

/**
 * Utility class for scheduling reminders using AlarmManager
 */
object ReminderScheduler {
    
    private const val TAG = "ReminderScheduler"
    
    /**
     * Schedule a task reminder
     */
    fun scheduleTaskReminder(
        context: Context,
        taskId: String,
        taskTitle: String,
        taskDescription: String?,
        reminderTime: Long
    ) {
        if (reminderTime <= System.currentTimeMillis()) {
            Log.w(TAG, "Cannot schedule reminder in the past")
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Check if we can schedule exact alarms on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms - permission not granted")
                // Fall back to inexact alarm
                scheduleInexactAlarm(
                    context = context,
                    entityType = ReminderBroadcastReceiver.TYPE_TASK,
                    entityId = taskId,
                    title = taskTitle,
                    description = taskDescription,
                    reminderTime = reminderTime
                )
                return
            }
        }
        
        val intent = createReminderIntent(
            context = context,
            entityType = ReminderBroadcastReceiver.TYPE_TASK,
            entityId = taskId,
            title = taskTitle,
            description = taskDescription
        )
        
        val pendingIntent = createPendingIntent(context, taskId.hashCode(), intent)
        
        try {
            // Use setExactAndAllowWhileIdle for better reliability
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Task reminder scheduled for $taskTitle at $reminderTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule task reminder", e)
        }
    }
    
    /**
     * Schedule a habit reminder
     */
    fun scheduleHabitReminder(
        context: Context,
        habitId: String,
        habitName: String,
        reminderTime: Long
    ) {
        if (reminderTime <= System.currentTimeMillis()) {
            Log.w(TAG, "Cannot schedule reminder in the past")
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Check if we can schedule exact alarms on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms - permission not granted")
                // Fall back to inexact alarm
                scheduleInexactAlarm(
                    context = context,
                    entityType = ReminderBroadcastReceiver.TYPE_HABIT,
                    entityId = habitId,
                    title = habitName,
                    description = null,
                    reminderTime = reminderTime
                )
                return
            }
        }
        
        val intent = createReminderIntent(
            context = context,
            entityType = ReminderBroadcastReceiver.TYPE_HABIT,
            entityId = habitId,
            title = habitName,
            description = null
        )
        
        val pendingIntent = createPendingIntent(context, habitId.hashCode(), intent)
        
        try {
            // Use setExactAndAllowWhileIdle for better reliability
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Habit reminder scheduled for $habitName at $reminderTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule habit reminder", e)
        }
    }
    
    /**
     * Cancel a task reminder
     */
    fun cancelTaskReminder(context: Context, taskId: String) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Task reminder cancelled for $taskId")
        }
    }
    
    /**
     * Cancel a habit reminder
     */
    fun cancelHabitReminder(context: Context, habitId: String) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Habit reminder cancelled for $habitId")
        }
    }
    
    /**
     * Create a reminder intent with all necessary data
     */
    private fun createReminderIntent(
        context: Context,
        entityType: String,
        entityId: String,
        title: String,
        description: String?
    ): Intent {
        return Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_ENTITY_TYPE, entityType)
            putExtra(ReminderBroadcastReceiver.EXTRA_ENTITY_ID, entityId)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, title)
            description?.let { putExtra(ReminderBroadcastReceiver.EXTRA_DESCRIPTION, it) }
        }
    }
    
    /**
     * Create a PendingIntent for the reminder
     */
    private fun createPendingIntent(context: Context, requestCode: Int, intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Schedule an inexact alarm (fallback for devices that don't allow exact alarms)
     */
    private fun scheduleInexactAlarm(
        context: Context,
        entityType: String,
        entityId: String,
        title: String,
        description: String?,
        reminderTime: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = createReminderIntent(
            context = context,
            entityType = entityType,
            entityId = entityId,
            title = title,
            description = description
        )
        
        val pendingIntent = createPendingIntent(context, entityId.hashCode(), intent)
        
        try {
            // Use setWindow for inexact alarms (allows 5 minute window)
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                5 * 60 * 1000L, // 5 minute window
                pendingIntent
            )
            
            Log.d(TAG, "Inexact alarm scheduled for $title at $reminderTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule inexact alarm", e)
        }
    }
}

