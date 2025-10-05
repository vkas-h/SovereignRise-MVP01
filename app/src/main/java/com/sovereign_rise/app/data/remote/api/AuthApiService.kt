package com.sovereign_rise.app.data.remote.api

import com.sovereign_rise.app.data.remote.dto.AuthResponse
import com.sovereign_rise.app.data.remote.dto.UpdateProfileRequest
import com.sovereign_rise.app.data.remote.dto.UpdateProfileResponse
import com.sovereign_rise.app.data.remote.dto.UserDto
import com.sovereign_rise.app.data.remote.dto.VerifyTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * Retrofit API service interface for authentication endpoints.
 * Communicates with the Next.js backend for user authentication and profile management.
 */
interface AuthApiService {

    /**
     * Verifies a Firebase ID token with the backend and retrieves user profile data.
     * The backend will verify the token with Firebase Admin SDK and return user data from CockroachDB.
     */
    @POST("/api/auth/verify")
    suspend fun verifyToken(
        @Body request: VerifyTokenRequest
    ): Response<AuthResponse>

    /**
     * Retrieves the current user's profile data.
     * Auth token is automatically added by the auth interceptor.
     */
    @GET("/api/user/profile")
    suspend fun getUserProfile(): Response<UserDto>

    /**
     * Logs out the user and invalidates the session on the backend.
     * Auth token is automatically added by the auth interceptor.
     */
    @POST("/api/auth/logout")
    suspend fun logout(): Response<Unit>
    
    /**
     * Updates the current user's profile (username and/or photo URL).
     * Auth token is automatically added by the auth interceptor.
     */
    @PUT("/api/user/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<UpdateProfileResponse>
}
