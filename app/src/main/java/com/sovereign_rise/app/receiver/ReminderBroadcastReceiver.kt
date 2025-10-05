package com.sovereign_rise.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sovereign_rise.app.util.NotificationHelper

/**
 * BroadcastReceiver for handling scheduled reminders
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ReminderReceiver"
        
        // Intent extras
        const val EXTRA_ENTITY_TYPE = "entity_type"
        const val EXTRA_ENTITY_ID = "entity_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "description"
        
        // Entity types
        const val TYPE_TASK = "task"
        const val TYPE_HABIT = "habit"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Reminder received")
        
        val entityType = intent.getStringExtra(EXTRA_ENTITY_TYPE) ?: return
        val entityId = intent.getStringExtra(EXTRA_ENTITY_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val description = intent.getStringExtra(EXTRA_DESCRIPTION)
        
        Log.d(TAG, "Showing reminder for $entityType: $title")
        
        when (entityType) {
            TYPE_TASK -> NotificationHelper.showTaskReminder(
                context = context,
                taskId = entityId,
                taskTitle = title,
                taskDescription = description
            )
            TYPE_HABIT -> NotificationHelper.showHabitReminder(
                context = context,
                habitId = entityId,
                habitName = title
            )
        }
    }
}

