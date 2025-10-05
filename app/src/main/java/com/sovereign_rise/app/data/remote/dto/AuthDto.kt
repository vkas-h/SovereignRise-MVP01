package com.sovereign_rise.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.sovereign_rise.app.domain.model.User

/**
 * Request DTO for token verification.
 */
data class VerifyTokenRequest(
    @SerializedName("idToken")
    val idToken: String
)

/**
 * Response DTO for authentication operations.
 */
data class AuthResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("user")
    val user: UserDto? = null
)

/**
 * DTO representing user data from the backend API.
 */
data class UserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("currentStreak")
    val streakDays: Int,
    @SerializedName("photoUrl")
    val profileImageUrl: String? = null
)

/**
 * Extension function to convert UserDto to domain User model.
 */
fun UserDto.toUser(): User {
    return User(
        id = this.id,
        username = this.username,
        email = this.email,
        streakDays = this.streakDays,
        profileImageUrl = this.profileImageUrl
    )
}

/**
 * Request DTO for updating user profile.
 */
data class UpdateProfileRequest(
    @SerializedName("username")
    val username: String? = null,
    @SerializedName("photoUrl")
    val photoUrl: String? = null
)

/**
 * Response DTO for profile update.
 */
data class UpdateProfileResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("user")
    val user: UserDto? = null
)