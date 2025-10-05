package com.sovereign_rise.app.domain.repository

import com.sovereign_rise.app.domain.model.SyncStatus
import com.sovereign_rise.app.domain.model.User

/**
 * Repository interface for user profile operations with caching support.
 */
interface UserRepository {
    
    /**
     * Gets user profile (from cache or network).
     */
    suspend fun getUserProfile(userId: String): User
    
    /**
     * Updates user profile.
     */
    suspend fun updateUserProfile(user: User): User
    
    /**
     * Gets currently logged-in user.
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * Forces refresh from server.
     */
    suspend fun refreshProfile(userId: String): User
    
    /**
     * Clears cached profile data.
     */
    suspend fun clearCache()
    
    /**
     * Checks if cache is valid (not expired).
     */
    suspend fun isCacheValid(userId: String): Boolean
    
    /**
     * Gets profile sync status.
     */
    suspend fun getSyncStatus(userId: String): SyncStatus
    
    /**
     * Adds XP to user (legacy method - consider migrating to updateUserProfile).
     */
    suspend fun addXp(userId: String, xpGained: Int): User
    
    /**
     * Updates user currency balances (legacy method - consider migrating to updateUserProfile).
     */
    suspend fun updateCurrency(userId: String, aetherAmount: Int, crownAmount: Int): User
    
    /**
     * Updates user profile (username and/or profile photo).
     * @param username New username (null to keep existing)
     * @param photoUrl New photo URL (null to keep existing)
     * @return Updated User object
     */
    suspend fun updateProfile(username: String? = null, photoUrl: String? = null): User
}
