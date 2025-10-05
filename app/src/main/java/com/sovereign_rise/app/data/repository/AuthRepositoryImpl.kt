package com.sovereign_rise.app.data.repository

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.sovereign_rise.app.data.local.TokenDataStore
import com.sovereign_rise.app.data.local.dao.UserDao
import com.sovereign_rise.app.data.local.entity.UserEntity
import com.sovereign_rise.app.data.local.entity.toUser
import com.sovereign_rise.app.data.remote.NetworkModule
import com.sovereign_rise.app.data.remote.api.AuthApiService
import com.sovereign_rise.app.data.remote.dto.VerifyTokenRequest
import com.sovereign_rise.app.data.remote.dto.toUser
import com.sovereign_rise.app.domain.model.SyncStatus
import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.domain.repository.AuthRepository
import com.sovereign_rise.app.util.ConnectivityObserver
import com.sovereign_rise.app.util.Constants
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Implementation of AuthRepository with Firebase Auth integration.
 * Orchestrates Firebase authentication, backend API verification, and local token storage.
 * 
 * @param firebaseAuth Firebase Auth instance for authentication operations
 * @param authApiService Retrofit API service for backend communication
 * @param tokenDataStore DataStore for persisting authentication tokens
 * @param context Application context for Credential Manager
 * @param userDao Room DAO for user caching
 * @param connectivityObserver Observer for network connectivity
 */
class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val authApiService: AuthApiService,
    private val tokenDataStore: TokenDataStore,
    private val context: Context,
    private val userDao: UserDao,
    private val connectivityObserver: ConnectivityObserver
) : AuthRepository {
    
    companion object {
        // Firebase Web Client ID for Google Sign-In (One Tap)
        private const val WEB_CLIENT_ID = Constants.FIREBASE_WEB_CLIENT_ID
        private const val TOKEN_EXPIRY_HOURS = Constants.TOKEN_EXPIRY_HOURS
    }
    
    override suspend fun login(email: String, password: String): User {
        try {
            // Sign in with Firebase
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            
            // Get Firebase ID token
            val token = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
                ?: throw Exception("Failed to get Firebase ID token")
            
            // Verify token with backend and get user data
            val response = authApiService.verifyToken(VerifyTokenRequest(token))
            
            if (!response.isSuccessful || response.body()?.success != true) {
                throw Exception(response.body()?.message ?: "Authentication failed")
            }
            
            val userDto = response.body()?.user
                ?: throw Exception("User data not found in response")
            
            // Save token to local storage
            val expiryTimestamp = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(TOKEN_EXPIRY_HOURS)
            tokenDataStore.saveToken(token, userDto.id, isGuest = false, expiryTimestamp)
            
            // Set auth token for network interceptor
            NetworkModule.setAuthToken(token)
            
            // Cache user in Room database
            val user = userDto.toUser()
            val userEntity = UserEntity(
                id = user.id,
                username = user.username,
                email = user.email,
                streakDays = user.streakDays,
                profileImageUrl = user.profileImageUrl,
                isOfflineGuest = false,
                syncStatus = SyncStatus.SYNCED.name,
                lastSyncedAt = System.currentTimeMillis(),
                serverUpdatedAt = System.currentTimeMillis(),
                localUpdatedAt = System.currentTimeMillis(),
                cachedAt = System.currentTimeMillis()
            )
            userDao.insertUser(userEntity)
            
            // Return domain user model
            return user
            
        } catch (e: FirebaseAuthException) {
            throw Exception("Invalid credentials: ${e.message}")
        } catch (e: Exception) {
            throw Exception("Login failed: ${e.message}")
        }
    }
    
    override suspend fun register(username: String, email: String, password: String): User {
        try {
            // Create user with Firebase
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            
            // Update Firebase profile with username
            val profileUpdates = userProfileChangeRequest {
                displayName = username
            }
            firebaseAuth.currentUser?.updateProfile(profileUpdates)?.await()
            
            // Get Firebase ID token
            val token = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
                ?: throw Exception("Failed to get Firebase ID token")
            
            // Verify token with backend (backend will create user record in CockroachDB)
            val response = authApiService.verifyToken(VerifyTokenRequest(token))
            
            if (!response.isSuccessful || response.body()?.success != true) {
                throw Exception(response.body()?.message ?: "Registration failed")
            }
            
            val userDto = response.body()?.user
                ?: throw Exception("User data not found in response")
            
            // Save token to local storage
            val expiryTimestamp = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(TOKEN_EXPIRY_HOURS)
            tokenDataStore.saveToken(token, userDto.id, isGuest = false, expiryTimestamp)
            
            // Set auth token for network interceptor
            NetworkModule.setAuthToken(token)
            
            // Cache user in Room database
            val user = userDto.toUser()
            val userEntity = UserEntity(
                id = user.id,
                username = user.username,
                email = user.email,
                streakDays = user.streakDays,
                profileImageUrl = user.profileImageUrl,
                isOfflineGuest = false,
                syncStatus = SyncStatus.SYNCED.name,
                lastSyncedAt = System.currentTimeMillis(),
                serverUpdatedAt = System.currentTimeMillis(),
                localUpdatedAt = System.currentTimeMillis(),
                cachedAt = System.currentTimeMillis()
            )
            userDao.insertUser(userEntity)
            
            return user
            
        } catch (e: FirebaseAuthException) {
            throw Exception("Registration failed: ${e.message}")
        } catch (e: Exception) {
            throw Exception("Registration failed: ${e.message}")
        }
    }
    
    override suspend fun loginWithGoogle(activityContext: Activity): User {
        try {
            // Create Credential Manager
            val credentialManager = CredentialManager.create(activityContext)
            
            // Configure Google ID option for One Tap Sign-In
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            // Get credential from Credential Manager (shows One Tap UI)
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            
            // Extract Google ID token from credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val googleIdToken = googleIdTokenCredential.idToken
            
            // Sign in to Firebase with Google credential
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            firebaseAuth.signInWithCredential(firebaseCredential).await()
            
            // Get Firebase ID token
            val token = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
                ?: throw Exception("Failed to get Firebase ID token")
            
            // Verify token with backend
            val response = authApiService.verifyToken(VerifyTokenRequest(token))
            
            if (!response.isSuccessful || response.body()?.success != true) {
                throw Exception(response.body()?.message ?: "Google Sign-In failed")
            }
            
            val userDto = response.body()?.user
                ?: throw Exception("User data not found in response")
            
            // Save token to local storage
            val expiryTimestamp = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(TOKEN_EXPIRY_HOURS)
            tokenDataStore.saveToken(token, userDto.id, isGuest = false, expiryTimestamp)
            
            // Set auth token for network interceptor
            NetworkModule.setAuthToken(token)
            
            // Cache user in Room database
            val user = userDto.toUser()
            val userEntity = UserEntity(
                id = user.id,
                username = user.username,
                email = user.email,
                streakDays = user.streakDays,
                profileImageUrl = user.profileImageUrl,
                isOfflineGuest = false,
                syncStatus = SyncStatus.SYNCED.name,
                lastSyncedAt = System.currentTimeMillis(),
                serverUpdatedAt = System.currentTimeMillis(),
                localUpdatedAt = System.currentTimeMillis(),
                cachedAt = System.currentTimeMillis()
            )
            userDao.insertUser(userEntity)
            
            return user
            
        } catch (e: GetCredentialException) {
            throw Exception("Google Sign-In cancelled or failed: ${e.message}")
        } catch (e: Exception) {
            throw Exception("Google Sign-In failed: ${e.message}")
        }
    }
    
    override suspend fun loginAsGuest(): User {
        try {
            // Sign in anonymously with Firebase
            firebaseAuth.signInAnonymously().await()
            
            // Get Firebase ID token
            val token = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
                ?: throw Exception("Failed to get Firebase ID token")
            
            // Verify token with backend (backend will create guest user record)
            val response = authApiService.verifyToken(VerifyTokenRequest(token))
            
            if (!response.isSuccessful || response.body()?.success != true) {
                throw Exception(response.body()?.message ?: "Guest login failed")
            }
            
            val userDto = response.body()?.user
                ?: throw Exception("User data not found in response")
            
            // Save token to local storage with guest flag
            val expiryTimestamp = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(TOKEN_EXPIRY_HOURS)
            tokenDataStore.saveToken(token, userDto.id, isGuest = true, expiryTimestamp)
            
            // Set auth token for network interceptor
            NetworkModule.setAuthToken(token)
            
            // Cache user in Room database
            val user = userDto.toUser()
            val userEntity = UserEntity(
                id = user.id,
                username = user.username,
                email = user.email,
                streakDays = user.streakDays,
                profileImageUrl = user.profileImageUrl,
                isOfflineGuest = false, // This is an online guest (Firebase anonymous)
                syncStatus = SyncStatus.SYNCED.name,
                lastSyncedAt = System.currentTimeMillis(),
                serverUpdatedAt = System.currentTimeMillis(),
                localUpdatedAt = System.currentTimeMillis(),
                cachedAt = System.currentTimeMillis()
            )
            userDao.insertUser(userEntity)
            
            return user
            
        } catch (e: Exception) {
            throw Exception("Guest login failed: ${e.message}")
        }
    }
    
    override suspend fun upgradeGuestAccount(username: String, email: String, password: String): User {
        try {
            // Verify current user is a guest (anonymous)
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null || !currentUser.isAnonymous) {
                throw Exception("No guest account to upgrade")
            }
            
            // Create email credential
            val credential = EmailAuthProvider.getCredential(email, password)
            
            // Link credential to anonymous account
            currentUser.linkWithCredential(credential).await()
            
            // Update profile with username
            val profileUpdates = userProfileChangeRequest {
                displayName = username
            }
            currentUser.updateProfile(profileUpdates).await()
            
            // Get new Firebase ID token
            val token = currentUser.getIdToken(true).await()?.token
                ?: throw Exception("Failed to get Firebase ID token")
            
            // Verify token with backend (backend will update user from guest to full account)
            val response = authApiService.verifyToken(VerifyTokenRequest(token))
            
            if (!response.isSuccessful || response.body()?.success != true) {
                throw Exception(response.body()?.message ?: "Account upgrade failed")
            }
            
            val userDto = response.body()?.user
                ?: throw Exception("User data not found in response")
            
            // Save token to local storage (no longer a guest)
            val expiryTimestamp = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(TOKEN_EXPIRY_HOURS)
            tokenDataStore.saveToken(token, userDto.id, isGuest = false, expiryTimestamp)
            
            // Set auth token for network interceptor
            NetworkModule.setAuthToken(token)
            
            return userDto.toUser()
            
        } catch (e: FirebaseAuthException) {
            throw Exception("Account upgrade failed: ${e.message}")
        } catch (e: Exception) {
            throw Exception("Account upgrade failed: ${e.message}")
        }
    }
    
    override suspend fun refreshToken(): String? {
        return try {
            val token = firebaseAuth.currentUser?.getIdToken(true)?.await()?.token
            if (token != null) {
                // Update cached token in DataStore
                val userId = tokenDataStore.getUserId()
                val isGuest = tokenDataStore.isGuest()
                if (userId != null) {
                    val expiryTimestamp = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(TOKEN_EXPIRY_HOURS)
                    tokenDataStore.saveToken(token, userId, isGuest, expiryTimestamp)
                    
                    // Set auth token for network interceptor
                    NetworkModule.setAuthToken(token)
                }
            }
            token
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun logout() {
        try {
            // Get token for backend logout call
            val token = tokenDataStore.getToken()
            
            // Sign out from Firebase
            firebaseAuth.signOut()
            
            // Clear local token
            tokenDataStore.clearToken()
            
            // Clear auth token from network interceptor
            NetworkModule.setAuthToken(null)
            
            // Optionally notify backend of logout
            if (token != null) {
                try {
                    authApiService.logout()
                } catch (e: Exception) {
                    // Ignore backend logout errors
                }
            }
        } catch (e: Exception) {
            throw Exception("Logout failed: ${e.message}")
        }
    }
    
    override suspend fun getCurrentUser(): User? {
        return try {
            // First check: if no userId stored, truly logged out
            val userId = tokenDataStore.getUserId()
            if (userId == null) {
                android.util.Log.d("AuthRepo", "No userId found - user not logged in")
                return null
            }
            
            // Second check: if offline guest, return cached user
            if (tokenDataStore.isOfflineGuest()) {
                return getCachedUser()
            }
            
            // Third check: check connectivity
            val isOnline = connectivityObserver.isOnline()
            
            // If offline, always return cached data (even if token expired)
            if (!isOnline) {
                android.util.Log.d("AuthRepo", "Offline - returning cached user")
                return getCachedUser()
            }
            
            // Get token
            var token = tokenDataStore.getToken()
            
            // If online but no token, return cached data
            if (token == null) {
                return getCachedUser()
            }
            
            // Fetch user profile from backend
            android.util.Log.d("AuthRepo", "Fetching user profile from API...")
            var response = authApiService.getUserProfile()
            
            android.util.Log.d("AuthRepo", "API Response code: ${response.code()}, Success: ${response.isSuccessful}")
            if (!response.isSuccessful) {
                // If we get a 401 (unauthorized), try to refresh the token first
                if (response.code() == 401) {
                    android.util.Log.w("AuthRepo", "Token is invalid (401), attempting to refresh...")
                    
                    // Attempt to refresh the token
                    val refreshedToken = refreshToken()
                    
                    if (refreshedToken != null) {
                        // Token refresh succeeded, retry the profile fetch with new token
                        android.util.Log.d("AuthRepo", "Token refreshed successfully, retrying profile fetch")
                        token = refreshedToken
                        response = authApiService.getUserProfile()
                        
                        // If still getting 401 after refresh, clear token and return null
                        if (!response.isSuccessful && response.code() == 401) {
                            android.util.Log.w("AuthRepo", "Profile fetch still failed after token refresh, clearing token")
                            tokenDataStore.clearToken()
                            return null
                        }
                    } else {
                        // Token refresh failed, clear token and return null
                        android.util.Log.w("AuthRepo", "Token refresh failed, clearing token")
                        tokenDataStore.clearToken()
                        return null
                    }
                } else {
                    android.util.Log.e("AuthRepo", "Profile fetch failed with code: ${response.code()}, returning cached user")
                    return getCachedUser()
                }
            }
            
            val user = response.body()?.toUser()
            
            android.util.Log.d("AuthRepo", "Received user from API: ${user?.username}, Streak: ${user?.streakDays}")
            
            // Update cache with fresh data
            if (user != null) {
                val userEntity = UserEntity(
                    id = user.id,
                    username = user.username,
                    email = user.email,
                    streakDays = user.streakDays,
                    profileImageUrl = user.profileImageUrl,
                    isOfflineGuest = false,
                    syncStatus = SyncStatus.SYNCED.name,
                    lastSyncedAt = System.currentTimeMillis(),
                    serverUpdatedAt = System.currentTimeMillis(),
                    localUpdatedAt = System.currentTimeMillis(),
                    cachedAt = System.currentTimeMillis()
                )
                userDao.insertUser(userEntity)
            }
            
            user
            
        } catch (e: Exception) {
            // On network/connection errors, return cached user
            android.util.Log.e("AuthRepo", "Error fetching current user: ${e.message}, returning cached user")
            getCachedUser()
        }
    }
    
    override suspend fun isAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null && tokenDataStore.isTokenValid()
    }
    
    override suspend fun isGuest(): Boolean {
        return tokenDataStore.isGuest()
    }
    
    /**
     * Creates a local-only guest session without Firebase/backend calls.
     * Generates a local guest user ID and stores in Room database.
     * Reuses existing offline guest session if present to avoid accumulating unused profiles.
     */
    override suspend fun loginAsGuestOffline(): User {
        try {
            // Check if there's already an existing offline guest session
            val existingUserId = tokenDataStore.getUserId()
            val isExistingOfflineGuest = tokenDataStore.isOfflineGuest()
            
            if (isExistingOfflineGuest && existingUserId != null) {
                // Load and return existing offline guest user
                val existingUser = userDao.getUserById(existingUserId)
                if (existingUser != null) {
                    android.util.Log.d("AuthRepo", "Reusing existing offline guest session: $existingUserId")
                    return existingUser.toUser()
                }
            }
            
            // Delete any old offline guest profiles to prevent accumulation
            userDao.deleteOfflineGuests()
            
            // Generate local guest user ID
            val guestUserId = "guest_${java.util.UUID.randomUUID()}"
            
            // Create User object with guest ID
            val guestUser = User(
                id = guestUserId,
                username = "Guest",
                email = "",
                streakDays = 0,
                profileImageUrl = null,
                isGuest = true
            )
            
            // Save to Room database
            val userEntity = UserEntity(
                id = guestUserId,
                username = "Guest",
                email = "",
                streakDays = 0,
                profileImageUrl = null,
                isOfflineGuest = true,
                syncStatus = SyncStatus.PENDING.name,
                lastSyncedAt = null,
                serverUpdatedAt = null,
                localUpdatedAt = System.currentTimeMillis(),
                cachedAt = System.currentTimeMillis()
            )
            userDao.insertUser(userEntity)
            
            // Save offline guest session to DataStore
            tokenDataStore.saveOfflineGuestSession(guestUserId)
            
            android.util.Log.d("AuthRepo", "Created offline guest session: $guestUserId")
            
            return guestUser
            
        } catch (e: Exception) {
            throw Exception("Failed to create offline guest session: ${e.message}")
        }
    }
    
    /**
     * Retrieves user from Room database cache.
     */
    override suspend fun getCachedUser(): User? {
        return try {
            val userId = tokenDataStore.getUserId() ?: return null
            val userEntity = userDao.getUserById(userId)
            userEntity?.toUser()
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "Error getting cached user: ${e.message}")
            null
        }
    }
    
    /**
     * Checks if user has valid local session (token or offline guest).
     */
    override suspend fun hasValidLocalSession(): Boolean {
        return tokenDataStore.hasValidSession()
    }
    
    /**
     * Checks if current user is an offline guest.
     */
    override suspend fun isOfflineGuest(): Boolean {
        return tokenDataStore.isOfflineGuest()
    }
}

