package com.sovereign_rise.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore wrapper class for managing authentication tokens and related data.
 * Provides a clean abstraction for token persistence and retrieval.
 */
class TokenDataStore(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")
        
        private val KEY_FIREBASE_TOKEN = stringPreferencesKey("firebase_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_IS_GUEST = booleanPreferencesKey("is_guest")
        private val KEY_TOKEN_EXPIRY = longPreferencesKey("token_expiry")
    }
    
    /**
     * Exposes the underlying DataStore<Preferences> for repositories that need direct access.
     */
    val dataStore: DataStore<Preferences>
        get() = context.dataStore

    /**
     * Saves authentication token and associated data atomically.
     */
    suspend fun saveToken(
        token: String,
        userId: String,
        isGuest: Boolean,
        expiryTimestamp: Long
    ) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_FIREBASE_TOKEN] = token
                preferences[KEY_USER_ID] = userId
                preferences[KEY_IS_GUEST] = isGuest
                preferences[KEY_TOKEN_EXPIRY] = expiryTimestamp
            }
        } catch (e: Exception) {
            // Log error in production
            e.printStackTrace()
        }
    }

    /**
     * Retrieves the Firebase ID token.
     */
    suspend fun getToken(): String? {
        return try {
            context.dataStore.data.map { preferences ->
                preferences[KEY_FIREBASE_TOKEN]
            }.first()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Retrieves the user ID.
     */
    suspend fun getUserId(): String? {
        return try {
            context.dataStore.data.map { preferences ->
                preferences[KEY_USER_ID]
            }.first()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Checks if the user is in guest mode.
     */
    suspend fun isGuest(): Boolean {
        return try {
            context.dataStore.data.map { preferences ->
                preferences[KEY_IS_GUEST] ?: false
            }.first()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Checks if the token exists and hasn't expired.
     * Does NOT check for offline guest - use hasValidSession() instead for complete check.
     */
    suspend fun isTokenValid(): Boolean {
        return try {
            val currentTime = System.currentTimeMillis()
            context.dataStore.data.map { preferences ->
                val token = preferences[KEY_FIREBASE_TOKEN]
                val expiry = preferences[KEY_TOKEN_EXPIRY] ?: 0L
                token != null && currentTime < expiry
            }.first()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves an offline guest session without Firebase token.
     * Sets isGuest = true, userId = provided guestUserId, no token/expiry.
     */
    suspend fun saveOfflineGuestSession(guestUserId: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_USER_ID] = guestUserId
                preferences[KEY_IS_GUEST] = true
                // No token or expiry for offline guests
                preferences.remove(KEY_FIREBASE_TOKEN)
                preferences.remove(KEY_TOKEN_EXPIRY)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if current session is an offline guest.
     * Returns true if userId starts with "guest_" and isGuest = true.
     */
    suspend fun isOfflineGuest(): Boolean {
        return try {
            context.dataStore.data.map { preferences ->
                val userId = preferences[KEY_USER_ID]
                val isGuest = preferences[KEY_IS_GUEST] ?: false
                userId != null && userId.startsWith("guest_") && isGuest
            }.first()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Checks if user has a valid session (either valid token OR offline guest OR has userId for offline access).
     * This is the primary method to check if a user can access the app.
     * Note: For offline access, we allow cached data even if token expired, as long as userId exists.
     */
    suspend fun hasValidSession(): Boolean {
        return try {
            // Valid session = offline guest OR valid token OR has userId (for offline cached access)
            isOfflineGuest() || isTokenValid() || (getUserId() != null)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Clears all authentication data (used for logout).
     */
    suspend fun clearToken() {
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(KEY_FIREBASE_TOKEN)
                preferences.remove(KEY_USER_ID)
                preferences.remove(KEY_IS_GUEST)
                preferences.remove(KEY_TOKEN_EXPIRY)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
