package com.sovereign_rise.app.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sovereign_rise.app.BuildConfig
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.protocol.User

object ErrorLogger {

    private const val TAG = "ErrorLogger"

    /**
     * Log exception to both Crashlytics and Sentry
     */
    fun logException(
        exception: Throwable,
        context: String? = null,
        extras: Map<String, String>? = null
    ) {
        try {
            // Log to Crashlytics
            if (context != null) {
                FirebaseCrashlytics.getInstance().log("Context: $context")
            }
            extras?.forEach { (key, value) ->
                FirebaseCrashlytics.getInstance().setCustomKey(key, value)
            }
            FirebaseCrashlytics.getInstance().recordException(exception)
            
            // Log to Sentry
            Sentry.captureException(exception) { scope ->
                if (context != null) {
                    scope.setTag("context", context)
                }
                extras?.forEach { (key, value) ->
                    scope.setExtra(key, value)
                }
            }
            
            // Also log to Android log in debug builds
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Exception in $context: ${exception.message}", exception)
            }
        } catch (e: Exception) {
            // Ignore errors in error reporting
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error logging exception", e)
            }
        }
    }

    /**
     * Log non-fatal error
     */
    fun logError(message: String, extras: Map<String, String>? = null) {
        try {
            // Log to Crashlytics
            FirebaseCrashlytics.getInstance().log("ERROR: $message")
            extras?.forEach { (key, value) ->
                FirebaseCrashlytics.getInstance().setCustomKey(key, value)
            }
            
            // Log to Sentry
            Sentry.captureMessage(message, SentryLevel.ERROR) { scope ->
                extras?.forEach { (key, value) ->
                    scope.setExtra(key, value)
                }
            }
            
            if (BuildConfig.DEBUG) {
                Log.e(TAG, message)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error logging error", e)
            }
        }
    }

    /**
     * Log warning
     */
    fun logWarning(message: String) {
        try {
            FirebaseCrashlytics.getInstance().log("WARNING: $message")
            Sentry.captureMessage(message, SentryLevel.WARNING)
            
            if (BuildConfig.DEBUG) {
                Log.w(TAG, message)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error logging warning", e)
            }
        }
    }

    /**
     * Log info (debug builds only)
     */
    fun logInfo(message: String) {
        if (BuildConfig.DEBUG) {
            try {
                FirebaseCrashlytics.getInstance().log("INFO: $message")
                Log.i(TAG, message)
            } catch (e: Exception) {
                Log.e(TAG, "Error logging info", e)
            }
        }
    }

    /**
     * Set user context for error tracking
     */
    fun setUser(userId: String, username: String, level: Int) {
        try {
            // Set for Crashlytics
            FirebaseCrashlytics.getInstance().setUserId(userId)
            FirebaseCrashlytics.getInstance().setCustomKey("username", username)
            FirebaseCrashlytics.getInstance().setCustomKey("level", level)
            
            // Set for Sentry
            val user = User().apply {
                id = userId
                this.username = username
                // Store level in user data
                this.data = mapOf("level" to level.toString())
            }
            Sentry.setUser(user)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error setting user context", e)
            }
        }
    }

    /**
     * Clear user context (on logout)
     */
    fun clearUser() {
        try {
            FirebaseCrashlytics.getInstance().setUserId("")
            Sentry.setUser(null)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error clearing user context", e)
            }
        }
    }

    /**
     * Add breadcrumb for debugging
     */
    fun addBreadcrumb(message: String, category: String, data: Map<String, Any>? = null) {
        try {
            // Log to Crashlytics
            FirebaseCrashlytics.getInstance().log("[$category] $message")
            
            // Add to Sentry
            val breadcrumb = Breadcrumb().apply {
                this.message = message
                this.category = category
                this.level = SentryLevel.INFO
                data?.forEach { (key, value) ->
                    setData(key, value)
                }
            }
            Sentry.addBreadcrumb(breadcrumb)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error adding breadcrumb", e)
            }
        }
    }
}

