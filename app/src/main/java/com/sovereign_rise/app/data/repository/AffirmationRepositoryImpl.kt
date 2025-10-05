package com.sovereign_rise.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sovereign_rise.app.data.remote.api.AIApiService
import com.sovereign_rise.app.data.remote.dto.GenerateAffirmationRequest
import com.sovereign_rise.app.domain.model.*
import com.sovereign_rise.app.domain.repository.AffirmationRepository
import com.sovereign_rise.app.util.Constants
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.UUID

class AffirmationRepositoryImpl(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val aiApiService: AIApiService
) : AffirmationRepository {
    
    companion object {
        private val PREFERRED_TONE_KEY = stringPreferencesKey(Constants.PREF_AFFIRMATION_TONE)
        private val LAST_AFFIRMATION_TIME_KEY = longPreferencesKey(Constants.PREF_LAST_AFFIRMATION_TIME)
        private val AFFIRMATIONS_TODAY_COUNT_KEY = intPreferencesKey(Constants.PREF_AFFIRMATIONS_TODAY_COUNT)
        private val LAST_AFFIRMATION_DATE_KEY = longPreferencesKey("last_affirmation_date")
        private val LAST_SHOWN_TEMPLATE_KEY = stringPreferencesKey("last_shown_template")
    }
    
    private val templates = createTemplates()
    
    override suspend fun getAffirmationTemplates(tone: AffirmationTone, context: AffirmationContext): List<AffirmationTemplate> {
        return templates.filter { it.tone == tone && it.context == context }
    }
    
    override suspend fun getAllTemplates(): List<AffirmationTemplate> {
        return templates
    }
    
    override suspend fun generateAffirmation(context: AffirmationContext, variables: Map<String, String>): Affirmation? {
        val userId = "current_user" // Will be replaced with actual user ID
        if (!canShowAffirmation(userId)) return null
        
        val preferredTone = getUserPreferredTone(userId)
        return selectAffirmation(preferredTone, context, variables)
    }
    
    override suspend fun selectAffirmation(tone: AffirmationTone, context: AffirmationContext, variables: Map<String, String>): Affirmation? {
        return try {
            // Call the backend AI API to generate a real affirmation
            val request = GenerateAffirmationRequest(
                context = context.name,
                tone = tone.name,
                variables = variables
            )
            
            val response = aiApiService.generateAffirmation(request)
            
            if (response.isSuccessful && response.body() != null) {
                val affirmationResponse = response.body()!!
                Affirmation(
                    id = affirmationResponse.id,
                    message = affirmationResponse.message,
                    tone = AffirmationTone.valueOf(affirmationResponse.tone),
                    context = AffirmationContext.valueOf(affirmationResponse.context),
                    variables = variables
                )
            } else {
                Log.e("AffirmationRepo", "API call failed: ${response.code()}, ${response.message()}")
                // Fallback to template-based affirmation
                selectAffirmationFromTemplate(tone, context, variables)
            }
        } catch (e: Exception) {
            Log.e("AffirmationRepo", "Error generating AI affirmation: ${e.message}", e)
            // Fallback to template-based affirmation
            selectAffirmationFromTemplate(tone, context, variables)
        }
    }
    
    /**
     * Fallback method using templates when AI generation fails
     */
    private suspend fun selectAffirmationFromTemplate(tone: AffirmationTone, context: AffirmationContext, variables: Map<String, String>): Affirmation? {
        val matchingTemplates = getAffirmationTemplates(tone, context)
        if (matchingTemplates.isEmpty()) return null
        
        val preferences = dataStore.data.first()
        val lastShownTemplate = preferences[LAST_SHOWN_TEMPLATE_KEY]
        
        // Avoid showing the same template twice in a row
        val availableTemplates = matchingTemplates.filter { it.template != lastShownTemplate }
        val selectedTemplate = (if (availableTemplates.isNotEmpty()) availableTemplates else matchingTemplates).random()
        
        val message = populateTemplate(selectedTemplate.template, variables)
        
        val affirmation = Affirmation(
            id = UUID.randomUUID().toString(),
            message = message,
            tone = tone,
            context = context,
            variables = variables
        )
        
        // Store last shown template
        dataStore.edit { prefs ->
            prefs[LAST_SHOWN_TEMPLATE_KEY] = selectedTemplate.template
        }
        
        return affirmation
    }
    
    override suspend fun canShowAffirmation(userId: String): Boolean {
        val preferences = dataStore.data.first()
        val count = getAffirmationsShownToday(userId)
        
        if (count >= Constants.MAX_AFFIRMATIONS_PER_DAY) {
            return false
        }
        
        return !isInCooldown(userId)
    }
    
    override suspend fun getAffirmationsShownToday(userId: String): Int {
        val preferences = dataStore.data.first()
        val count = preferences[AFFIRMATIONS_TODAY_COUNT_KEY] ?: 0
        val lastDate = preferences[LAST_AFFIRMATION_DATE_KEY] ?: 0L
        
        val currentDate = getDayStart(System.currentTimeMillis())
        val lastAffirmationDate = getDayStart(lastDate)
        
        return if (currentDate != lastAffirmationDate) 0 else count
    }
    
    override suspend fun recordAffirmationShown(userId: String, affirmation: Affirmation) {
        val currentTime = System.currentTimeMillis()
        
        dataStore.edit { preferences ->
            val currentCount = getAffirmationsShownToday(userId)
            preferences[AFFIRMATIONS_TODAY_COUNT_KEY] = currentCount + 1
            preferences[LAST_AFFIRMATION_TIME_KEY] = currentTime
            preferences[LAST_AFFIRMATION_DATE_KEY] = currentTime
        }
    }
    
    override suspend fun resetDailyCount(userId: String) {
        dataStore.edit { preferences ->
            preferences[AFFIRMATIONS_TODAY_COUNT_KEY] = 0
        }
    }
    
    override suspend fun getLastAffirmationTime(userId: String): Long? {
        val preferences = dataStore.data.first()
        return preferences[LAST_AFFIRMATION_TIME_KEY]
    }
    
    override suspend fun isInCooldown(userId: String): Boolean {
        val lastTime = getLastAffirmationTime(userId) ?: return false
        val cooldownMillis = Constants.AFFIRMATION_COOLDOWN_HOURS * 60 * 60 * 1000
        return (System.currentTimeMillis() - lastTime) < cooldownMillis
    }
    
    override suspend fun getUserPreferredTone(userId: String): AffirmationTone {
        val preferences = dataStore.data.first()
        val toneName = preferences[PREFERRED_TONE_KEY] ?: AffirmationTone.MOTIVATIONAL.name
        return AffirmationTone.valueOf(toneName)
    }
    
    override suspend fun setUserPreferredTone(userId: String, tone: AffirmationTone) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_TONE_KEY] = tone.name
        }
    }
    
    override suspend fun fetchAffirmationsFromBackend(context: AffirmationContext): List<Affirmation> {
        // TODO: Implement backend fetch when API is ready
        return emptyList()
    }
    
    override suspend fun syncDeliveryLog(deliveries: List<AffirmationDelivery>) {
        // TODO: Implement backend sync when API is ready
    }
    
    // Helper functions
    
    private fun populateTemplate(template: String, variables: Map<String, String>): String {
        var result = template
        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
    
    private fun getDayStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    private fun createTemplates(): List<AffirmationTemplate> {
        return listOf(
            // MOTIVATIONAL + TASK_COMPLETED
            AffirmationTemplate(
                template = "You just proved momentum > excuses. Keep going!",
                tone = AffirmationTone.MOTIVATIONAL,
                context = AffirmationContext.TASK_COMPLETED,
                requiredVariables = emptyList()
            ),
            AffirmationTemplate(
                template = "That's {xp} XP earned! You're unstoppable today!",
                tone = AffirmationTone.MOTIVATIONAL,
                context = AffirmationContext.TASK_COMPLETED,
                requiredVariables = listOf("xp")
            ),
            AffirmationTemplate(
                template = "Action taken. Progress made. You're building something real.",
                tone = AffirmationTone.MOTIVATIONAL,
                context = AffirmationContext.TASK_COMPLETED,
                requiredVariables = emptyList()
            ),
            
            // PHILOSOPHICAL + TASK_COMPLETED
            AffirmationTemplate(
                template = "Another brick laid. Your fortress of discipline is rising.",
                tone = AffirmationTone.PHILOSOPHICAL,
                context = AffirmationContext.TASK_COMPLETED,
                requiredVariables = emptyList()
            ),
            AffirmationTemplate(
                template = "Progress, not perfection. You're walking the path.",
                tone = AffirmationTone.PHILOSOPHICAL,
                context = AffirmationContext.TASK_COMPLETED,
                requiredVariables = emptyList()
            ),
            AffirmationTemplate(
                template = "Small steps, taken daily, move mountains eventually.",
                tone = AffirmationTone.PHILOSOPHICAL,
                context = AffirmationContext.TASK_COMPLETED,
                requiredVariables = emptyList()
            ),
            
            // PRACTICAL + TASK_COMPLETED
            AffirmationTemplate(
                template = "Task completed. {xp} XP earned. Next.",
                tone = AffirmationTone.PRACTICAL,
                context = AffirmationContext.TASK_COMPLETED,
                requiredVariables = listOf("xp")
            ),
            AffirmationTemplate(
                template = "Done. Moving forward.",
                tone = AffirmationTone.PRACTICAL,
                context = AffirmationContext.TASK_COMPLETED,
                requiredVariables = emptyList()
            ),
            AffirmationTemplate(
                template = "One more off the list. Keep the momentum.",
                tone = AffirmationTone.PRACTICAL,
                context = AffirmationContext.TASK_COMPLETED,
                requiredVariables = emptyList()
            ),
            
            // HUMOROUS + TASK_COMPLETED
            AffirmationTemplate(
                template = "Another one bites the dust! 🎵",
                tone = AffirmationTone.HUMOROUS,
                context = AffirmationContext.TASK_COMPLETED,
                requiredVariables = emptyList()
            ),
            AffirmationTemplate(
                template = "Task eliminated. Your todo list never stood a chance.",
                tone = AffirmationTone.HUMOROUS,
                context = AffirmationContext.TASK_COMPLETED,
                requiredVariables = emptyList()
            ),
            
            // MOTIVATIONAL + HABIT_CHECKED
            AffirmationTemplate(
                template = "{streak_days} days strong! You're building an empire of good habits!",
                tone = AffirmationTone.MOTIVATIONAL,
                context = AffirmationContext.HABIT_CHECKED,
                requiredVariables = listOf("streak_days")
            ),
            AffirmationTemplate(
                template = "Consistency is your superpower. Another day, another win!",
                tone = AffirmationTone.MOTIVATIONAL,
                context = AffirmationContext.HABIT_CHECKED,
                requiredVariables = emptyList()
            ),
            
            // PHILOSOPHICAL + HABIT_CHECKED
            AffirmationTemplate(
                template = "Habits are the compound interest of self-improvement. You're investing wisely.",
                tone = AffirmationTone.PHILOSOPHICAL,
                context = AffirmationContext.HABIT_CHECKED,
                requiredVariables = emptyList()
            ),
            
            // PRACTICAL + HABIT_CHECKED
            AffirmationTemplate(
                template = "Habit checked. Streak maintained. {xp} XP earned.",
                tone = AffirmationTone.PRACTICAL,
                context = AffirmationContext.HABIT_CHECKED,
                requiredVariables = listOf("xp")
            ),
            
            // MOTIVATIONAL + PERFECT_DAY
            AffirmationTemplate(
                template = "PERFECT DAY! You completed everything! This is legendary status!",
                tone = AffirmationTone.MOTIVATIONAL,
                context = AffirmationContext.PERFECT_DAY,
                requiredVariables = emptyList()
            ),
            
            // MOTIVATIONAL + STREAK_MILESTONE
            AffirmationTemplate(
                template = "{streak_days} days of choosing growth over comfort. You're unstoppable!",
                tone = AffirmationTone.MOTIVATIONAL,
                context = AffirmationContext.STREAK_MILESTONE,
                requiredVariables = listOf("streak_days")
            )
        )
    }
}

