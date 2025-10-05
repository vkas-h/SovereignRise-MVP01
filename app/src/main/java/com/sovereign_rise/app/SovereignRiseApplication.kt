package com.sovereign_rise.app

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sovereign_rise.app.util.NotificationHelper
import io.sentry.android.core.SentryAndroid

class SovereignRiseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase Crashlytics
        initializeCrashlytics()

        // Initialize Sentry
        initializeSentry()

        // Initialize WorkManager (will be auto-configured with default settings)
        
        // Create notification channels
        NotificationHelper.createNotificationChannels(this)
        
        // Set up global exception handler
        setupGlobalExceptionHandler()
    }

    private fun initializeCrashlytics() {
        // Disable Crashlytics collection in debug builds
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        
        // Set custom keys for context
        FirebaseCrashlytics.getInstance().setCustomKey("app_version", BuildConfig.VERSION_NAME)
        FirebaseCrashlytics.getInstance().setCustomKey("version_code", BuildConfig.VERSION_CODE)
    }

    private fun initializeSentry() {
        // Skip Sentry initialization in debug builds
        // We already have Firebase Crashlytics for error tracking
        if (BuildConfig.DEBUG) {
            return
        }
        
        // TODO: Initialize Sentry for production builds when you have a DSN
        // Uncomment and configure when you're ready to use Sentry in production
        /*
        SentryAndroid.init(this) { options ->
            // Set your Sentry DSN here
            options.dsn = "YOUR_SENTRY_DSN_HERE"
            
            // Set environment
            options.environment = "production"
            
            // Set release version
            options.release = "${BuildConfig.VERSION_NAME}@${BuildConfig.VERSION_CODE}"
            
            // Set traces sample rate
            options.tracesSampleRate = 0.2
            
            // Filter sensitive data before sending
            options.beforeSend = io.sentry.SentryOptions.BeforeSendCallback { event, _ ->
                // Remove any sensitive user data here if needed
                event
            }
        }
        */
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log to Crashlytics
                FirebaseCrashlytics.getInstance().recordException(throwable)
                FirebaseCrashlytics.getInstance().log("Uncaught exception on thread: ${thread.name}")
                
                // Log to Sentry (if enabled)
                io.sentry.Sentry.captureException(throwable)
            } catch (e: Exception) {
                // Ignore errors in error reporting
            } finally {
                // Call default handler to crash the app
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}

