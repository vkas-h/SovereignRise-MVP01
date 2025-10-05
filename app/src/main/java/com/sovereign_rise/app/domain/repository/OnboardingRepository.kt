package com.sovereign_rise.app.domain.repository

import com.sovereign_rise.app.domain.model.TutorialDay
import com.sovereign_rise.app.domain.model.TutorialState
import com.sovereign_rise.app.domain.model.TutorialTask

interface OnboardingRepository {
    
    // Onboarding state
    suspend fun isOnboardingCompleted(): Boolean
    
    suspend fun markOnboardingCompleted()
    
    suspend fun resetOnboarding()
    
    // Tutorial state
    suspend fun getTutorialState(): TutorialState
    
    suspend fun getCurrentTutorialDay(): TutorialDay
    
    suspend fun getTutorialTasks(day: TutorialDay): List<TutorialTask>
    
    suspend fun completeTutorialTask(taskId: String)
    
    suspend fun advanceTutorialDay()
    
    suspend fun isTutorialCompleted(): Boolean
    
    // Feature unlocking
    suspend fun getUnlockedFeatures(): List<String>
    
    suspend fun unlockFeature(feature: String)
    
    suspend fun isFeatureUnlockedByTutorial(feature: String): Boolean
    
    // Task limits
    suspend fun getMaxTasksForCurrentDay(): Int
    
    suspend fun canCreateMoreTasks(currentCount: Int): Boolean
}

