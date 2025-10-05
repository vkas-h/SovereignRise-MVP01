package com.sovereign_rise.app.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sovereign_rise.app.domain.model.TutorialDay
import com.sovereign_rise.app.domain.model.TutorialState
import com.sovereign_rise.app.domain.model.TutorialTask
import com.sovereign_rise.app.domain.repository.OnboardingRepository
import com.sovereign_rise.app.util.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OnboardingRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : OnboardingRepository {

    companion object {
        private const val TAG = "OnboardingRepositoryImpl"
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey(Constants.PREF_ONBOARDING_COMPLETED)
        private val TUTORIAL_DAY_KEY = stringPreferencesKey(Constants.PREF_TUTORIAL_DAY)
        private val COMPLETED_TASKS_KEY = stringPreferencesKey("tutorial_completed_tasks")
        private val UNLOCKED_FEATURES_KEY = stringPreferencesKey("tutorial_unlocked_features")
        private val TUTORIAL_COMPLETED_KEY = booleanPreferencesKey(Constants.PREF_TUTORIAL_COMPLETED)
    }

    private val gson = Gson()

    override suspend fun isOnboardingCompleted(): Boolean {
        return try {
            dataStore.data.map { prefs ->
                prefs[ONBOARDING_COMPLETED_KEY] ?: false
            }.first()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking onboarding status", e)
            false
        }
    }

    override suspend fun markOnboardingCompleted() {
        try {
            dataStore.edit { prefs ->
                prefs[ONBOARDING_COMPLETED_KEY] = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking onboarding completed", e)
        }
    }

    override suspend fun resetOnboarding() {
        try {
            dataStore.edit { prefs ->
                prefs[ONBOARDING_COMPLETED_KEY] = false
                prefs[TUTORIAL_COMPLETED_KEY] = false
                prefs[TUTORIAL_DAY_KEY] = TutorialDay.DAY_1.name
                prefs[COMPLETED_TASKS_KEY] = gson.toJson(emptyList<String>())
                prefs[UNLOCKED_FEATURES_KEY] = gson.toJson(emptyList<String>())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting onboarding", e)
        }
    }

    override suspend fun getTutorialState(): TutorialState {
        return try {
            val currentDay = getCurrentTutorialDay()
            val completedTasks = getCompletedTasksInternal()
            val unlockedFeatures = getUnlockedFeatures()
            val isCompleted = isTutorialCompleted()
            
            val maxTasks = when (currentDay) {
                TutorialDay.DAY_1 -> Constants.TUTORIAL_DAY_1_TASK_LIMIT
                TutorialDay.DAY_2, TutorialDay.DAY_3 -> Constants.TUTORIAL_DAY_3_TASK_LIMIT
                TutorialDay.DAY_7 -> Constants.TUTORIAL_DAY_7_TASK_LIMIT
                TutorialDay.COMPLETED -> Constants.TUTORIAL_DAY_14_TASK_LIMIT
            }
            
            TutorialState(
                currentDay = currentDay,
                completedTasks = completedTasks,
                unlockedFeatures = unlockedFeatures,
                maxTasksAllowed = maxTasks,
                isCompleted = isCompleted
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tutorial state", e)
            TutorialState(
                currentDay = TutorialDay.DAY_1,
                completedTasks = emptyList(),
                unlockedFeatures = emptyList(),
                maxTasksAllowed = Constants.TUTORIAL_DAY_1_TASK_LIMIT,
                isCompleted = false
            )
        }
    }

    override suspend fun getCurrentTutorialDay(): TutorialDay {
        return try {
            val dayName = dataStore.data.map { prefs ->
                prefs[TUTORIAL_DAY_KEY] ?: TutorialDay.DAY_1.name
            }.first()
            
            TutorialDay.valueOf(dayName)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current tutorial day", e)
            TutorialDay.DAY_1
        }
    }

    override suspend fun getTutorialTasks(day: TutorialDay): List<TutorialTask> {
        val completedTasks = getCompletedTasksInternal()
        
        return when (day) {
            TutorialDay.DAY_1 -> listOf(
                TutorialTask(
                    id = "tut_first_task",
                    title = "Create your first task",
                    description = "Add a task to get started with Sovereign Rise",
                    isCompleted = "tut_first_task" in completedTasks
                ),
                TutorialTask(
                    id = "tut_complete_task",
                    title = "Complete a task",
                    description = "Mark your first task as complete",
                    isCompleted = "tut_complete_task" in completedTasks
                ),
                TutorialTask(
                    id = "tut_set_reminder",
                    title = "Set a reminder",
                    description = "Add a reminder to a task",
                    isCompleted = "tut_set_reminder" in completedTasks
                )
            )
            TutorialDay.DAY_2 -> listOf(
                TutorialTask(
                    id = "tut_first_habit",
                    title = "Create your first habit",
                    description = "Start building daily habits",
                    isCompleted = "tut_first_habit" in completedTasks
                ),
                TutorialTask(
                    id = "tut_perfect_day",
                    title = "Complete all tasks",
                    description = "Achieve a perfect day",
                    isCompleted = "tut_perfect_day" in completedTasks
                )
            )
            TutorialDay.DAY_3 -> listOf(
                TutorialTask(
                    id = "tut_find_buddy",
                    title = "Connect with an accountability partner",
                    description = "Find someone to rise with",
                    isCompleted = "tut_find_buddy" in completedTasks
                )
            )
            TutorialDay.DAY_7 -> listOf(
                TutorialTask(
                    id = "tut_explore_guilds",
                    title = "Explore guilds",
                    description = "Check out the guild system",
                    isCompleted = "tut_explore_guilds" in completedTasks
                ),
                TutorialTask(
                    id = "tut_visit_market",
                    title = "Visit the market",
                    description = "Browse available items",
                    isCompleted = "tut_visit_market" in completedTasks
                )
            )
            TutorialDay.COMPLETED -> emptyList()
        }
    }

    override suspend fun completeTutorialTask(taskId: String) {
        try {
            val completedTasks = getCompletedTasksInternal().toMutableList()
            if (taskId !in completedTasks) {
                completedTasks.add(taskId)
                dataStore.edit { prefs ->
                    prefs[COMPLETED_TASKS_KEY] = gson.toJson(completedTasks)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error completing tutorial task", e)
        }
    }

    override suspend fun advanceTutorialDay() {
        try {
            val currentDay = getCurrentTutorialDay()
            val nextDay = when (currentDay) {
                TutorialDay.DAY_1 -> TutorialDay.DAY_2
                TutorialDay.DAY_2 -> TutorialDay.DAY_3
                TutorialDay.DAY_3 -> TutorialDay.DAY_7
                TutorialDay.DAY_7 -> TutorialDay.COMPLETED
                TutorialDay.COMPLETED -> TutorialDay.COMPLETED
            }
            
            dataStore.edit { prefs ->
                prefs[TUTORIAL_DAY_KEY] = nextDay.name
                if (nextDay == TutorialDay.COMPLETED) {
                    prefs[TUTORIAL_COMPLETED_KEY] = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error advancing tutorial day", e)
        }
    }

    override suspend fun isTutorialCompleted(): Boolean {
        return try {
            dataStore.data.map { prefs ->
                prefs[TUTORIAL_COMPLETED_KEY] ?: false
            }.first()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking tutorial completion", e)
            false
        }
    }

    override suspend fun getUnlockedFeatures(): List<String> {
        return try {
            val json = dataStore.data.map { prefs ->
                prefs[UNLOCKED_FEATURES_KEY] ?: gson.toJson(emptyList<String>())
            }.first()
            
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unlocked features", e)
            emptyList()
        }
    }

    override suspend fun unlockFeature(feature: String) {
        try {
            val features = getUnlockedFeatures().toMutableList()
            if (feature !in features) {
                features.add(feature)
                dataStore.edit { prefs ->
                    prefs[UNLOCKED_FEATURES_KEY] = gson.toJson(features)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking feature", e)
        }
    }

    override suspend fun isFeatureUnlockedByTutorial(feature: String): Boolean {
        return try {
            feature in getUnlockedFeatures()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking feature unlock", e)
            false
        }
    }

    override suspend fun getMaxTasksForCurrentDay(): Int {
        return try {
            val currentDay = getCurrentTutorialDay()
            when (currentDay) {
                TutorialDay.DAY_1 -> Constants.TUTORIAL_DAY_1_TASK_LIMIT
                TutorialDay.DAY_2, TutorialDay.DAY_3 -> Constants.TUTORIAL_DAY_3_TASK_LIMIT
                TutorialDay.DAY_7 -> Constants.TUTORIAL_DAY_7_TASK_LIMIT
                TutorialDay.COMPLETED -> Constants.TUTORIAL_DAY_14_TASK_LIMIT
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting max tasks", e)
            Constants.TUTORIAL_DAY_1_TASK_LIMIT
        }
    }

    override suspend fun canCreateMoreTasks(currentCount: Int): Boolean {
        return try {
            val maxTasks = getMaxTasksForCurrentDay()
            currentCount < maxTasks
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if can create more tasks", e)
            false
        }
    }

    private suspend fun getCompletedTasksInternal(): List<String> {
        return try {
            val json = dataStore.data.map { prefs ->
                prefs[COMPLETED_TASKS_KEY] ?: gson.toJson(emptyList<String>())
            }.first()
            
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting completed tasks", e)
            emptyList()
        }
    }
}

