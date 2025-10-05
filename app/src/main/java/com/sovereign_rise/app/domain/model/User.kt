package com.sovereign_rise.app.domain.model

/**
 * Domain model representing a user in the app.
 * Supports both authenticated users and guest users.
 */
data class User(
    val id: String,
    val username: String,
    val email: String,
    val streakDays: Int,
    val profileImageUrl: String? = null,
    val isGuest: Boolean = false
)
