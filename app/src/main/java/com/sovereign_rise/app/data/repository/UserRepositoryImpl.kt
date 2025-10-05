package com.sovereign_rise.app.data.repository

import android.util.Log
import com.sovereign_rise.app.data.local.dao.UserDao
import com.sovereign_rise.app.data.local.entity.toUser
import com.sovereign_rise.app.data.local.entity.toUserEntity
import com.sovereign_rise.app.data.remote.api.AuthApiService
import com.sovereign_rise.app.data.sync.SyncManager
import com.sovereign_rise.app.domain.model.SyncStatus
import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.domain.repository.UserRepository
import com.sovereign_rise.app.util.Constants

/**
 * Implementation of UserRepository with offline caching support.
 */
class UserRepositoryImpl(
    private val userDao: UserDao,
    private val authApiService: AuthApiService,
    private val syncManager: SyncManager
) : UserRepository {
    
    companion object {
        private const val TAG = "UserRepositoryImpl"
    }
    
    override suspend fun getUserProfile(userId: String): User {
        return try {
            // Check local cache first
            val cachedUser = userDao.getUserById(userId)
            
            // Check if cache is valid (< 7 days old)
            val cacheValid = cachedUser?.let {
                val cacheAge = System.currentTimeMillis() - it.cachedAt
                cacheAge < (Constants.OFFLINE_CACHE_DURATION_DAYS * 24 * 60 * 60 * 1000L)
            } ?: false
            
            if (cacheValid && cachedUser != null) {
                Log.d(TAG, "Returning cached user profile (streak: ${cachedUser.streakDays})")
                return cachedUser.toUser()
            }
            
            Log.d(TAG, "Cache invalid or expired, fetching from API...")
            // Try to fetch from API if online
            if (syncManager.isOnline()) {
                try {
                    val response = authApiService.getUserProfile()
                    if (response.isSuccessful) {
                        val userDto = response.body() ?: throw Exception("Empty response")
                        val user = User(
                            id = userDto.id,
                            username = userDto.username,
                            email = userDto.email,
                            streakDays = userDto.streakDays,
                            profileImageUrl = userDto.profileImageUrl
                        )
                        
                        // Update cache
                        val now = System.currentTimeMillis()
                        val entity = user.toUserEntity(
                            syncStatus = SyncStatus.SYNCED,
                            lastSyncedAt = now,
                            serverUpdatedAt = now,
                            localUpdatedAt = now,
                            cachedAt = now
                        )
                        userDao.insertUser(entity)
                        
                        return user
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching user from API", e)
                }
            }
            
            // If offline or API failed, return cached data even if expired
            if (cachedUser != null) {
                Log.d(TAG, "Returning expired cache (offline)")
                return cachedUser.toUser()
            }
            
            throw Exception("No cached user profile and unable to fetch from server")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile", e)
            throw e
        }
    }
    
    override suspend fun updateUserProfile(user: User): User {
        // Update local cache
        val entity = user.toUserEntity(
            SyncStatus.PENDING,
            null,
            null,
            System.currentTimeMillis(),
            System.currentTimeMillis()
        )
        userDao.updateUser(entity)
        
        // Enqueue sync action if needed
        // TODO: Implement user profile sync
        
        return user
    }
    
    override suspend fun getCurrentUser(): User? {
        return try {
            userDao.getCurrentUser()?.toUser()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user", e)
            null
        }
    }
    
    override suspend fun refreshProfile(userId: String): User {
        // Force fetch from server
        if (!syncManager.isOnline()) {
            throw Exception("Cannot refresh profile while offline")
        }
        
        val response = authApiService.getUserProfile()
        if (response.isSuccessful) {
            val userDto = response.body() ?: throw Exception("Empty response")
            val user =             User(
                id = userDto.id,
                username = userDto.username,
                email = userDto.email,
                streakDays = userDto.streakDays,
                profileImageUrl = userDto.profileImageUrl
            )
            
            // Update cache
            val now = System.currentTimeMillis()
            val entity = user.toUserEntity(
                syncStatus = SyncStatus.SYNCED,
                lastSyncedAt = now,
                serverUpdatedAt = now,
                localUpdatedAt = now,
                cachedAt = now
            )
            userDao.insertUser(entity)
            
            return user
        } else {
            throw Exception("Failed to refresh profile")
        }
    }
    
    override suspend fun clearCache() {
        userDao.deleteAll()
    }
    
    override suspend fun isCacheValid(userId: String): Boolean {
        val timestamp = userDao.getCacheTimestamp(userId) ?: return false
        val cacheAge = System.currentTimeMillis() - timestamp
        return cacheAge < (Constants.OFFLINE_CACHE_DURATION_DAYS * 24 * 60 * 60 * 1000L)
    }
    
    override suspend fun getSyncStatus(userId: String): SyncStatus {
        val user = userDao.getUserById(userId)
        return user?.let {
            SyncStatus.valueOf(it.syncStatus)
        } ?: SyncStatus.SYNCED
    }
    
    override suspend fun addXp(userId: String, xpGained: Int): User {
        // TODO: Implement when backend API is available
        throw NotImplementedError("AddXp not yet implemented - waiting for backend API")
    }
    
    override suspend fun updateCurrency(userId: String, aetherAmount: Int, crownAmount: Int): User {
        // TODO: Implement when backend API is available
        throw NotImplementedError("UpdateCurrency not yet implemented - waiting for backend API")
    }
    
    override suspend fun updateProfile(username: String?, photoUrl: String?): User {
        if (!syncManager.isOnline()) {
            throw Exception("Cannot update profile while offline")
        }
        
        // Prepare request
        val request = com.sovereign_rise.app.data.remote.dto.UpdateProfileRequest(
            username = username,
            photoUrl = photoUrl
        )
        
        // Call API (auth interceptor will add token)
        val response = authApiService.updateProfile(request)
        
        if (response.isSuccessful && response.body()?.success == true) {
            val userDto = response.body()?.user ?: throw Exception("Empty response")
            val user = User(
                id = userDto.id,
                username = userDto.username,
                email = userDto.email,
                streakDays = userDto.streakDays,
                profileImageUrl = userDto.profileImageUrl
            )
            
            // Update cache
            val now = System.currentTimeMillis()
            val entity = user.toUserEntity(
                syncStatus = SyncStatus.SYNCED,
                lastSyncedAt = now,
                serverUpdatedAt = now,
                localUpdatedAt = now,
                cachedAt = now
            )
            userDao.insertUser(entity)
            
            return user
        } else {
            val errorMessage = response.body()?.message ?: "Profile update failed"
            throw Exception(errorMessage)
        }
    }
}

