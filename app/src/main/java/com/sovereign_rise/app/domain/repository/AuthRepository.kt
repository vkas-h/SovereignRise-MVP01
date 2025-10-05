package com.sovereign_rise.app.domain.repository

import android.app.Activity
import com.sovereign_rise.app.domain.model.User

/**
 * Repository interface for authentication operations.
 * Defines the contract for authentication data operations.
 */
interface AuthRepository {
    
    /**
     * Logs in a user with credentials.
     * @param email User's email
     * @param password User's password
     * @return User object if successful
     */
    suspend fun login(email: String, password: String): User
    
    /**
     * Registers a new user.
     * @param username Desired username
     * @param email User's email
     * @param password User's password
     * @return User object if successful
     */
    suspend fun register(username: String, email: String, password: String): User
    
    /**
     * Logs in a user using Google Sign-In with One Tap.
     * Requires an Activity context for the Credential Manager API to show the One Tap Sign-In UI.
     * @param activityContext Activity context required for Credential Manager
     * @return User object if successful
     */
    suspend fun loginWithGoogle(activityContext: Activity): User
    
    /**
     * Logs in as a guest using Firebase anonymous authentication.
     * Guest users have limited features and can upgrade to a full account later.
     * @return User object representing the guest user
     */
    suspend fun loginAsGuest(): User
    
    /**
     * Upgrades a guest account to a full account by linking email/password credentials.
     * Can only be called when the current user is a guest (anonymous Firebase user).
     * @param username Desired username for the account
     * @param email User's email
     * @param password User's password
     * @return Updated User object with full account privileges
     */
    suspend fun upgradeGuestAccount(username: String, email: String, password: String): User
    
    /**
     * Refreshes the Firebase ID token and returns the new token.
     * Useful for ensuring the token is valid before making API calls.
     * @return Fresh Firebase ID token, or null if not authenticated
     */
    suspend fun refreshToken(): String?
    
    /**
     * Logs out the current user.
     */
    suspend fun logout()
    
    /**
     * Gets the currently authenticated user.
     * @return User object if authenticated, null otherwise
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * Checks if a user is currently logged in.
     * @return true if authenticated, false otherwise
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Checks if the current user is in guest mode.
     * @return true if user is a guest, false otherwise
     */
    suspend fun isGuest(): Boolean
    
    /**
     * Creates a local-only guest session without network calls.
     * Works completely offline without Firebase/backend interaction.
     * @return User object representing the offline guest user
     */
    suspend fun loginAsGuestOffline(): User
    
    /**
     * Retrieves user from Room database cache instead of backend API.
     * Used to support offline access for authenticated users.
     * @return Cached User object, or null if not found
     */
    suspend fun getCachedUser(): User?
    
    /**
     * Checks if user has valid local session (token or offline guest).
     * @return true if user has valid local session, false otherwise
     */
    suspend fun hasValidLocalSession(): Boolean
    
    /**
     * Checks if current user is an offline guest.
     * @return true if offline guest, false otherwise
     */
    suspend fun isOfflineGuest(): Boolean
}
