package com.sovereign_rise.app.domain.repository

import com.sovereign_rise.app.domain.model.Affirmation
import com.sovereign_rise.app.domain.model.AffirmationContext
import com.sovereign_rise.app.domain.model.AffirmationDelivery
import com.sovereign_rise.app.domain.model.AffirmationTemplate
import com.sovereign_rise.app.domain.model.AffirmationTone

/**
 * Repository interface for affirmation generation and delivery
 */
interface AffirmationRepository {
    
    // Template management
    suspend fun getAffirmationTemplates(tone: AffirmationTone, context: AffirmationContext): List<AffirmationTemplate>
    suspend fun getAllTemplates(): List<AffirmationTemplate>
    
    // Affirmation generation
    suspend fun generateAffirmation(context: AffirmationContext, variables: Map<String, String>): Affirmation?
    suspend fun selectAffirmation(tone: AffirmationTone, context: AffirmationContext, variables: Map<String, String>): Affirmation?
    
    // Daily limit enforcement
    suspend fun canShowAffirmation(userId: String): Boolean
    suspend fun getAffirmationsShownToday(userId: String): Int
    suspend fun recordAffirmationShown(userId: String, affirmation: Affirmation)
    suspend fun resetDailyCount(userId: String)
    
    // Cooldown management
    suspend fun getLastAffirmationTime(userId: String): Long?
    suspend fun isInCooldown(userId: String): Boolean
    
    // User preferences
    suspend fun getUserPreferredTone(userId: String): AffirmationTone
    suspend fun setUserPreferredTone(userId: String, tone: AffirmationTone)
    
    // Backend sync
    suspend fun fetchAffirmationsFromBackend(context: AffirmationContext): List<Affirmation>
    suspend fun syncDeliveryLog(deliveries: List<AffirmationDelivery>)
}

