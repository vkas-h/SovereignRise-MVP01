package com.sovereign_rise.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.usageTrackingDataStore by preferencesDataStore(name = "usage_tracking_settings")

/**
 * Manages usage tracking preferences
 */
class UsageTrackingPreferences(private val context: Context) {
    
    private val TRACKING_ENABLED_KEY = booleanPreferencesKey("tracking_enabled")
    
    val isTrackingEnabled: Flow<Boolean> = context.usageTrackingDataStore.data
        .map { preferences ->
            preferences[TRACKING_ENABLED_KEY] ?: false
        }
    
    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.usageTrackingDataStore.edit { preferences ->
            preferences[TRACKING_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun getTrackingEnabled(): Boolean {
        var enabled = false
        context.usageTrackingDataStore.data.map { preferences ->
            enabled = preferences[TRACKING_ENABLED_KEY] ?: false
        }.collect { }
        return enabled
    }
}

