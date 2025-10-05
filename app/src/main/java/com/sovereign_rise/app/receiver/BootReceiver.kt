package com.sovereign_rise.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sovereign_rise.app.di.AppModule
import com.sovereign_rise.app.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for handling device boot to reschedule reminders
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, rescheduling reminders")
            
            // Use a coroutine to fetch tasks and reschedule reminders
            val pendingResult = goAsync()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Get TaskRepository from DI
                    val taskRepository = AppModule.provideTaskRepository(context)
                    
                    // Fetch all tasks
                    val tasks = taskRepository.getTasks()
                    
                    // Reschedule reminders for tasks that have future reminder times
                    val currentTime = System.currentTimeMillis()
                    tasks.forEach { task ->
                        task.reminderTime?.let { reminderTime ->
                            if (reminderTime > currentTime) {
                                ReminderScheduler.scheduleTaskReminder(
                                    context = context,
                                    taskId = task.id,
                                    taskTitle = task.title,
                                    taskDescription = task.description,
                                    reminderTime = reminderTime
                                )
                            }
                        }
                    }
                    
                    Log.d(TAG, "Successfully rescheduled ${tasks.count { it.reminderTime != null && it.reminderTime > currentTime }} reminders")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling reminders after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

