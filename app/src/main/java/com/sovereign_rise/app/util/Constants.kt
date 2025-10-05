package com.sovereign_rise.app.util

object Constants {
    
    // App Info
    const val APP_NAME = "Sovereign Rise"
    const val APP_VERSION = "1.0.0"
    const val TIMEZONE_DEFAULT = "UTC"
    
    // Task System
    const val MAX_ACTIVE_TASKS = 12
    const val MAX_ACTIVE_HABITS = 10
    const val DAILY_RESET_HOUR_UTC = 0
    const val GRACE_PERIOD_MINUTES = 15
    
    // Habit Validation
    const val MIN_HABIT_NAME_LENGTH = 3
    const val MAX_HABIT_NAME_LENGTH = 50
    const val MAX_HABIT_DESCRIPTION_LENGTH = 200
    const val MIN_CUSTOM_INTERVAL_DAYS = 1
    
    // Streak Milestones
    const val MILESTONE_BRONZE_DAYS = 7
    const val MILESTONE_SILVER_DAYS = 30
    const val MILESTONE_GOLD_DAYS = 100
    
    // Task Validation
    const val MIN_TASK_TITLE_LENGTH = 3
    const val MAX_TASK_TITLE_LENGTH = 100
    const val MAX_TASK_DESCRIPTION_LENGTH = 500
    
    // Social
    const val MAX_ACCOUNTABILITY_PARTNERS = 3
    
    // Network
    const val NETWORK_TIMEOUT_SECONDS = 30L
    
    // Firebase Configuration
    const val FIREBASE_PROJECT_ID = "sovereign-rise-1e8dd"
    const val FIREBASE_PROJECT_NUMBER = "371457748669"
    const val FIREBASE_WEB_CLIENT_ID = "371457748669-vq5lk5drb5q092d75p7nlbl050fpakve.apps.googleusercontent.com"
    const val FIREBASE_ANDROID_CLIENT_ID = "371457748669-uualfg8am7tlh6hsnu1b2uu9s8uin6hf.apps.googleusercontent.com"
    const val FIREBASE_API_KEY = "AIzaSyBg1oqLYDPP5Staks7fqjeHMXRH8-OM5JQ"
    const val FIREBASE_STORAGE_BUCKET = "sovereign-rise-1e8dd.firebasestorage.app"
    
    // Auth Constants
    const val MIN_PASSWORD_LENGTH = 8
    const val MIN_USERNAME_LENGTH = 3
    const val MAX_USERNAME_LENGTH = 20
    const val TOKEN_EXPIRY_HOURS = 24L
    
    // API Endpoints
    const val ENDPOINT_AUTH_VERIFY = "/api/auth/verify"
    const val ENDPOINT_USER_PROFILE = "/api/user/profile"
    const val ENDPOINT_AUTH_LOGOUT = "/api/auth/logout"
    
    // API Endpoints - Tasks
    const val ENDPOINT_TASKS = "/api/tasks"
    const val ENDPOINT_TASK_BY_ID = "/api/tasks/{taskId}"
    const val ENDPOINT_COMPLETE_TASK = "/api/tasks/{taskId}/complete"
    const val ENDPOINT_DAILY_RESET = "/api/tasks/reset"
    
    // API Endpoints - Habits
    const val ENDPOINT_HABITS = "/api/habits"
    const val ENDPOINT_HABIT_BY_ID = "/api/habits/{habitId}"
    const val ENDPOINT_TICK_HABIT = "/api/habits/{habitId}/tick"
    const val ENDPOINT_CHECK_STREAK_BREAKS = "/api/habits/check-breaks"
    const val ENDPOINT_PROTECTION_ITEMS = "/api/habits/protection-items"
    
    // Error Messages
    const val ERROR_INVALID_EMAIL = "Please enter a valid email address"
    const val ERROR_PASSWORD_TOO_SHORT = "Password must be at least 8 characters"
    const val ERROR_USERNAME_TOO_SHORT = "Username must be at least 3 characters"
    const val ERROR_USERNAME_TOO_LONG = "Username must be at most 20 characters"
    const val ERROR_PASSWORDS_DONT_MATCH = "Passwords do not match"
    const val ERROR_NETWORK = "Network error. Please check your connection."
    const val ERROR_UNKNOWN = "An unexpected error occurred. Please try again."
    
    // Error Messages - Tasks
    const val ERROR_TASK_TITLE_TOO_SHORT = "Task title must be at least 3 characters"
    const val ERROR_TASK_TITLE_TOO_LONG = "Task title must be at most 100 characters"
    const val ERROR_TASK_DESCRIPTION_TOO_LONG = "Description must be at most 500 characters"
    const val ERROR_REMINDER_IN_PAST = "Reminder time must be in the future"
    const val ERROR_TASK_NOT_FOUND = "Task not found"
    const val ERROR_TASK_ALREADY_COMPLETED = "Task is already completed"
    
    // Error Messages - Habits
    const val ERROR_HABIT_NAME_TOO_SHORT = "Habit name must be at least 3 characters"
    const val ERROR_HABIT_NAME_TOO_LONG = "Habit name must be at most 50 characters"
    const val ERROR_HABIT_DESCRIPTION_TOO_LONG = "Description must be at most 200 characters"
    const val ERROR_HABIT_ALREADY_CHECKED = "Habit already checked today"
    const val ERROR_HABIT_NOT_FOUND = "Habit not found"
    const val ERROR_MAX_HABITS_REACHED = "Maximum of 10 active habits reached"
    const val ERROR_INVALID_INTERVAL = "Custom interval must be at least 1 day"
    
    // Preferences Keys (for DataStore)
    const val PREF_USER_TOKEN = "user_token"
    const val PREF_USER_ID = "user_id"
    const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
    
    // AI Features - Usage Stats
    const val USAGE_STATS_CHECK_INTERVAL_HOURS = 1L // How often to check usage stats
    const val USAGE_STATS_CACHE_DAYS = 30 // How many days of usage data to keep
    const val EXCESSIVE_USAGE_THRESHOLD_MINUTES = 30 // Trigger nudge after 30 min on distracting apps
    const val SCREEN_UNLOCK_SPIKE_THRESHOLD = 50 // Unlocks per hour that indicate distraction
    
    // AI Features - Smart Reminders
    const val MIN_COMPLETION_HISTORY_FOR_SUGGESTIONS = 5 // Need 5+ completions to suggest times
    const val SMART_REMINDER_CONFIDENCE_THRESHOLD = 0.7f // 70% confidence to show suggestion
    const val SMART_REMINDER_TIME_WINDOW_MINUTES = 30 // Suggest times within ±30 min of pattern
    
    // AI Features - Burnout Detection
    const val BURNOUT_CHECK_INTERVAL_HOURS = 24L // Check once per day
    const val BURNOUT_COMPLETION_RATE_THRESHOLD = 0.5f // <50% completion rate triggers warning
    const val BURNOUT_SNOOZE_THRESHOLD = 5 // 5+ snoozes per day indicates burnout
    const val BURNOUT_LATE_NIGHT_HOUR = 23 // Activity after 11 PM is late-night
    const val BURNOUT_RECOVERY_MODE_DAYS = 3 // Recovery mode lasts 3 days
    const val BURNOUT_RECOVERY_PENALTY_MULTIPLIER = 0.5f // 50% penalties during recovery
    
    // AI Features - Affirmations
    const val MAX_AFFIRMATIONS_PER_DAY = 3
    const val AFFIRMATION_COOLDOWN_HOURS = 4L // Min 4 hours between affirmations
    
    // AI Features - Nudges
    const val MAX_NUDGES_PER_DAY = 5
    const val NUDGE_COOLDOWN_MINUTES = 60L // Min 1 hour between nudges
    
    // Notification Channels
    const val NOTIFICATION_CHANNEL_REMINDERS = "smart_reminders"
    const val NOTIFICATION_CHANNEL_NUDGES = "ai_nudges"
    const val NOTIFICATION_CHANNEL_AFFIRMATIONS = "affirmations"
    const val NOTIFICATION_CHANNEL_BURNOUT = "burnout_alerts"
    
    // WorkManager Tags
    const val WORK_TAG_USAGE_STATS = "usage_stats_worker"
    const val WORK_TAG_BURNOUT_CHECK = "burnout_check_worker"
    const val WORK_TAG_SMART_REMINDERS = "smart_reminders_worker"
    
    // API Endpoints - AI Features
    const val ENDPOINT_AI_USAGE_STATS = "/api/ai/usage-stats"
    const val ENDPOINT_AI_SMART_REMINDERS = "/api/ai/smart-reminders"
    const val ENDPOINT_AI_BURNOUT = "/api/ai/burnout"
    const val ENDPOINT_AI_AFFIRMATIONS = "/api/ai/affirmations"
    
    // Preferences Keys - AI Features
    const val PREF_USAGE_STATS_ENABLED = "usage_stats_enabled"
    const val PREF_SMART_REMINDERS_ENABLED = "smart_reminders_enabled"
    const val PREF_BURNOUT_DETECTION_ENABLED = "burnout_detection_enabled"
    const val PREF_AI_NUDGES_ENABLED = "ai_nudges_enabled"
    const val PREF_AFFIRMATION_TONE = "affirmation_tone" // motivational/philosophical/practical/humorous
    const val PREF_LAST_AFFIRMATION_TIME = "last_affirmation_time"
    const val PREF_AFFIRMATIONS_TODAY_COUNT = "affirmations_today_count"
    const val PREF_RECOVERY_MODE_ACTIVE = "recovery_mode_active"
    const val PREF_RECOVERY_MODE_START_TIME = "recovery_mode_start_time"
    
    // Offline Mode Constants
    const val OFFLINE_CACHE_DURATION_DAYS = 7
    const val MAX_CACHE_SIZE_MB = 50
    const val SYNC_RETRY_ATTEMPTS = 3
    const val SYNC_RETRY_DELAY_SECONDS = 5L
    const val SYNC_BATCH_SIZE = 20
    
    // Sync Queue Constants
    const val MAX_QUEUED_ACTIONS = 100
    const val SYNC_CHECK_INTERVAL_MINUTES = 15L
    
    // Sync Priority Values
    const val PRIORITY_TASK_COMPLETION = 1
    const val PRIORITY_HABIT_TICK = 2
    const val PRIORITY_STREAK_UPDATE = 3
    const val PRIORITY_TASK_UPDATE = 4
    const val PRIORITY_TASK_CREATE = 5
    const val PRIORITY_HABIT_UPDATE = 6
    const val PRIORITY_HABIT_CREATE = 7
    const val PRIORITY_TASK_DELETE = 8
    const val PRIORITY_HABIT_DELETE = 9
    
    // WorkManager Tags - Offline Mode
    const val WORK_TAG_SYNC_ENGINE = "sync_engine_worker"
    const val WORK_TAG_CACHE_CLEANUP = "cache_cleanup_worker"
    
    // Preferences Keys - Offline Mode
    const val PREF_LAST_SYNC_TIME = "last_sync_time"
    const val PREF_PENDING_SYNC_COUNT = "pending_sync_count"
    const val PREF_OFFLINE_MODE_ENABLED = "offline_mode_enabled"
    
    // Analytics - Chart Periods
    const val ANALYTICS_PERIOD_7_DAYS = 7
    const val ANALYTICS_PERIOD_30_DAYS = 30
    const val ANALYTICS_PERIOD_90_DAYS = 90
    
    // Analytics - Metrics
    const val METRIC_STREAK = "streak_length"
    const val METRIC_TASKS_COMPLETED = "tasks_completed_daily"
    const val METRIC_HABITS_COMPLETED = "habits_completed_daily"
    const val METRIC_SCREEN_TIME = "screen_time_total"
    const val METRIC_PHONE_UNLOCKS = "phone_unlocks"
    
    // Onboarding
    const val ONBOARDING_SCREEN_COUNT = 5
    const val TUTORIAL_DAY_1_TASK_LIMIT = 3
    const val TUTORIAL_DAY_3_TASK_LIMIT = 5
    const val TUTORIAL_DAY_7_TASK_LIMIT = 8
    const val TUTORIAL_DAY_14_TASK_LIMIT = 12
    
    // API Endpoints - Analytics & Retention
    const val ENDPOINT_ANALYTICS_STREAK = "/api/analytics/streak"
    const val ENDPOINT_ANALYTICS_TASKS = "/api/analytics/tasks"
    const val ENDPOINT_INSIGHTS_WEEKLY = "/api/ai/insights"
    const val ENDPOINT_ANALYTICS = "/api/analytics"
    
    // Preferences Keys - Onboarding & Tutorial
    const val PREF_TUTORIAL_DAY = "tutorial_day"
    const val PREF_TUTORIAL_COMPLETED = "tutorial_completed"
    const val PREF_LOGIN_STREAK = "login_streak"
    const val PREF_LAST_LOGIN_DATE = "last_login_date"
    const val PREF_RETENTION_BONUSES_CLAIMED = "retention_bonuses_claimed"
    const val PREF_LAST_INSIGHTS_FETCH = "last_insights_fetch"
}
