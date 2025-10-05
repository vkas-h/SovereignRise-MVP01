package com.sovereign_rise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sovereign_rise.app.domain.model.SyncStatus
import com.sovereign_rise.app.domain.model.User

/**
 * Room entity for User profile caching.
 * Supports both authenticated users and offline guest users.
 */
@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: String, // Can store guest IDs like "guest_uuid"
    val username: String,
    val email: String, // Can be empty for guests
    val streakDays: Int,
    val profileImageUrl: String?, // Nullable for guests
    val isOfflineGuest: Boolean = false, // Distinguishes offline guests from Firebase guests
    // Sync metadata
    val syncStatus: String,
    val lastSyncedAt: Long?,
    val serverUpdatedAt: Long?,
    val localUpdatedAt: Long,
    val cachedAt: Long
)

/**
 * Extension function to convert UserEntity to User domain model.
 */
fun UserEntity.toUser(): User {
    return User(
        id = id,
        username = username,
        email = email,
        streakDays = streakDays,
        profileImageUrl = profileImageUrl
    )
}

/**
 * Extension function to convert User domain model to UserEntity.
 */
fun User.toUserEntity(
    syncStatus: SyncStatus,
    lastSyncedAt: Long?,
    serverUpdatedAt: Long?,
    localUpdatedAt: Long,
    cachedAt: Long,
    isOfflineGuest: Boolean = false
): UserEntity {
    return UserEntity(
        id = id,
        username = username,
        email = email,
        streakDays = streakDays,
        profileImageUrl = profileImageUrl,
        isOfflineGuest = isOfflineGuest,
        syncStatus = syncStatus.name,
        lastSyncedAt = lastSyncedAt,
        serverUpdatedAt = serverUpdatedAt,
        localUpdatedAt = localUpdatedAt,
        cachedAt = cachedAt
    )
}

