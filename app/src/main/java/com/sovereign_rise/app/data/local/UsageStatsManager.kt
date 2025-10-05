package com.sovereign_rise.app.data.local

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager as AndroidUsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import java.util.Calendar

/**
 * Manager for collecting phone usage statistics
 */
class UsageStatsManager(private val context: Context) {

    private val usageStatsManager: AndroidUsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? AndroidUsageStatsManager

    /**
     * Check if usage stats permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Get app usage data for a specific time range
     * Uses UsageEvents API for accurate foreground time tracking
     */
    fun getAppUsageStats(startTime: Long, endTime: Long): List<AppUsageData> {
        if (!hasUsageStatsPermission()) {
            return emptyList()
        }

        val events = usageStatsManager?.queryEvents(startTime, endTime) ?: return emptyList()

        // Track foreground time per app using events
        val appUsageMap = mutableMapOf<String, Long>()
        var currentApp: String? = null
        var appStartTime: Long = 0
        val processedEvents = mutableSetOf<String>() // Track event timestamps + package names
        
        while (events.hasNextEvent()) {
            val event = android.app.usage.UsageEvents.Event()
            events.getNextEvent(event)
            
            // Create unique event identifier to prevent double processing
            val eventId = "${event.timeStamp}_${event.packageName}_${event.eventType}"
            if (eventId in processedEvents) {
                continue
            }
            processedEvents.add(eventId)
            
            // Validate event timestamp is within our range
            if (event.timeStamp < startTime || event.timeStamp > endTime) {
                continue
            }
            
            when (event.eventType) {
                // App moved to foreground
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED,
                android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // Save previous app's time if there was one
                    if (currentApp != null && appStartTime > 0) {
                        val duration = event.timeStamp - appStartTime
                        if (duration > 0 && duration < 2 * 60 * 60 * 1000) { // Max 2 hours per session
                            appUsageMap[currentApp] = appUsageMap.getOrDefault(currentApp, 0L) + duration
                        }
                    }
                    // Start tracking new app
                    currentApp = event.packageName
                    appStartTime = event.timeStamp
                }
                
                // App moved to background or screen turned off
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND,
                android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (currentApp != null && appStartTime > 0) {
                        val duration = event.timeStamp - appStartTime
                        if (duration > 0 && duration < 2 * 60 * 60 * 1000) { // Max 2 hours per session
                            appUsageMap[currentApp] = appUsageMap.getOrDefault(currentApp, 0L) + duration
                        }
                        currentApp = null
                        appStartTime = 0
                    }
                }
            }
        }
        
        // Handle ongoing session (app still in foreground)
        if (currentApp != null && appStartTime > 0) {
            val duration = endTime - appStartTime
            if (duration > 0 && duration < 24 * 60 * 60 * 1000) { // Max 24 hours
                appUsageMap[currentApp] = appUsageMap.getOrDefault(currentApp, 0L) + duration
            }
        }

        // Normalize timestamps to midnight of the current day for consistent data storage
        val midnightTimestamp = Calendar.getInstance().apply {
            timeInMillis = startTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val result = appUsageMap
            .filter { it.value > 0 }
            .filter { !isSystemApp(it.key) } // Filter out system apps
            .map { (packageName, timeMillis) ->
                val appInfo = getAppInfo(packageName)
                val usageMinutes = (timeMillis / 1000 / 60).toInt()
                
                // Validate usage time - cap at 8 hours per day per app
                val validatedMinutes = usageMinutes.coerceAtMost(8 * 60)
                
                if (usageMinutes > validatedMinutes) {
                    android.util.Log.w("UsageStatsManager", "Capped unrealistic usage for $packageName: ${usageMinutes} -> ${validatedMinutes} min")
                }
                
                // Get app name with fallback priority: getAppInfo -> friendlyName mapping -> package name
                val displayName = appInfo?.name ?: getFriendlyAppName(packageName) ?: packageName
                
                AppUsageData(
                    packageName = packageName,
                    appName = displayName,
                    usageTimeMinutes = validatedMinutes,
                    category = appInfo?.category ?: "Other",
                    isProductive = isProductiveApp(packageName, appInfo?.category),
                    timestamp = midnightTimestamp // Use midnight timestamp for consistent updates
                )
            }
            .filter { it.usageTimeMinutes > 0 }
            .sortedByDescending { it.usageTimeMinutes }
        
        android.util.Log.d("UsageStatsManager", "Collected ${result.size} apps, total time: ${result.sumOf { it.usageTimeMinutes }} min, timestamp: ${java.util.Date(midnightTimestamp)}")
        
        return result
    }

    /**
     * Get daily summary statistics
     */
    fun getDailySummary(timestamp: Long): DailySummary {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        // Use current time as end time instead of full 24 hours
        // This ensures we only count screen time from midnight to NOW
        val endTime = System.currentTimeMillis()

        val appUsageList = getAppUsageStats(startTime, endTime)
        
        // Calculate screen time more accurately - cap total at 16 hours per day
        val rawTotalScreenTime = appUsageList.sumOf { it.usageTimeMinutes }
        val totalScreenTime = rawTotalScreenTime.coerceAtMost(16 * 60) // Max 16 hours
        
        if (rawTotalScreenTime > totalScreenTime) {
            android.util.Log.w("UsageStatsManager", "Capped unrealistic total screen time: ${rawTotalScreenTime} -> ${totalScreenTime} min")
        }
        
        val distractingTime = appUsageList.filter { !it.isProductive }.sumOf { it.usageTimeMinutes }
        val productiveTime = appUsageList.filter { it.isProductive }.sumOf { it.usageTimeMinutes }
        
        // Get unlock count (approximation based on app switches)
        val unlockCount = getUnlockCount(startTime, endTime)
        
        // Find peak usage hour
        val peakHour = findPeakUsageHour(startTime, endTime)

        val summary = DailySummary(
            timestamp = startTime, // Use start of day (midnight) as timestamp
            totalScreenTimeMinutes = totalScreenTime,
            unlockCount = unlockCount,
            distractingAppTimeMinutes = distractingTime,
            productiveAppTimeMinutes = productiveTime,
            peakUsageHour = peakHour
        )
        
        android.util.Log.d("UsageStatsManager", "Daily summary: ${totalScreenTime} min screen time, $unlockCount unlocks, timestamp: ${java.util.Date(startTime)}")
        
        return summary
    }

    /**
     * Get unlock count with improved accuracy
     * Matches Digital Wellbeing's counting method:
     * - Establishes initial screen state from events before midnight
     * - Only counts SCREEN_INTERACTIVE events (screen turns on) within the exact time range
     * - Only increments when screen goes from OFF -> ON
     */
    private fun getUnlockCount(startTime: Long, endTime: Long): Int {
        if (!hasUsageStatsPermission()) return 0
        
        // Query from a bit before startTime to establish initial screen state
        val queryStart = startTime - (60 * 60 * 1000) // 1 hour before
        val events = usageStatsManager?.queryEvents(queryStart, endTime) ?: return 0
        var unlockCount = 0
        var isScreenOn = false
        var screenOnCount = 0
        var screenOffCount = 0
        var initialScreenState = false
        
        while (events.hasNextEvent()) {
            val event = android.app.usage.UsageEvents.Event()
            events.getNextEvent(event)
            
            when (event.eventType) {
                android.app.usage.UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    // Before startTime: just track state, don't count
                    if (event.timeStamp < startTime) {
                        isScreenOn = true
                        initialScreenState = true
                    } 
                    // Within range [startTime, endTime]: count if screen was off
                    else if (event.timeStamp >= startTime && event.timeStamp <= endTime) {
                        screenOnCount++
                        if (!isScreenOn) {
                            unlockCount++
                            isScreenOn = true
                        }
                    }
                    // Events after endTime: ignore completely
                }
                android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    // Only update state for events within our query range
                    if (event.timeStamp < endTime) {
                        // Track screen off count only for events within the counting window
                        if (event.timeStamp >= startTime) {
                            screenOffCount++
                        }
                        isScreenOn = false
                    }
                    // Events after endTime: ignore completely
                }
            }
        }
        
        // Sanity check: max ~500 unlocks per day is reasonable
        if (unlockCount > 500) {
            android.util.Log.w("UsageStatsManager", "Unrealistic unlock count: $unlockCount, capping at 500")
            return 500
        }

        android.util.Log.d("UsageStatsManager", "📊 Unlock Analysis: count=$unlockCount | SCREEN_ON=$screenOnCount | SCREEN_OFF=$screenOffCount | Initial screen state at start: ${if (initialScreenState) "ON" else "OFF"}")
        return unlockCount
    }

    /**
     * Find peak usage hour using event-based tracking
     */
    private fun findPeakUsageHour(startTime: Long, endTime: Long): Int {
        if (!hasUsageStatsPermission()) return 12 // Default to noon
        
        val hourlyUsage = IntArray(24)
        val events = usageStatsManager?.queryEvents(startTime, endTime) ?: return 12
        
        var sessionStart = 0L
        var isInSession = false
        
        while (events.hasNextEvent()) {
            val event = android.app.usage.UsageEvents.Event()
            events.getNextEvent(event)
            
            when (event.eventType) {
                // Session started
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED,
                android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND,
                android.app.usage.UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    if (!isInSession) {
                        sessionStart = event.timeStamp
                        isInSession = true
                    }
                }
                // Session ended
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND,
                android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (isInSession && sessionStart > 0) {
                        val duration = event.timeStamp - sessionStart
                        val startHour = Calendar.getInstance().apply { 
                            timeInMillis = sessionStart 
                        }.get(Calendar.HOUR_OF_DAY)
                        val endHour = Calendar.getInstance().apply { 
                            timeInMillis = event.timeStamp 
                        }.get(Calendar.HOUR_OF_DAY)
                        
                        // Distribute time across hours if session spans multiple hours
                        if (startHour == endHour) {
                            hourlyUsage[startHour] += (duration / 1000 / 60).toInt()
                        } else {
                            // Simple distribution: add to start hour
                            hourlyUsage[startHour] += (duration / 1000 / 60).toInt()
                        }
                        
                        isInSession = false
                        sessionStart = 0
                    }
                }
            }
        }
        
        // Handle ongoing session
        if (isInSession && sessionStart > 0) {
            val duration = endTime - sessionStart
            val hour = Calendar.getInstance().apply { 
                timeInMillis = sessionStart 
            }.get(Calendar.HOUR_OF_DAY)
            hourlyUsage[hour] += (duration / 1000 / 60).toInt()
        }
        
        return hourlyUsage.indices.maxByOrNull { hourlyUsage[it] } ?: 12
    }

    /**
     * Get app information
     */
    private fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getCategoryName(appInfo.category)
            } else {
                "Other"
            }
            
            AppInfo(appName, category)
        } catch (e: PackageManager.NameNotFoundException) {
            // Fallback to known app names
            val friendlyName = getFriendlyAppName(packageName)
            if (friendlyName != null) {
                AppInfo(friendlyName, "Other")
            } else {
                null
            }
        }
    }
    
    /**
     * Get friendly app name from package name as fallback
     */
    private fun getFriendlyAppName(packageName: String): String? {
        return when (packageName) {
            "com.android.chrome" -> "Chrome"
            "com.google.android.apps.messaging" -> "Messages"
            "com.google.android.dialer" -> "Phone"
            "com.google.android.gm" -> "Gmail"
            "com.google.android.youtube" -> "YouTube"
            "com.facebook.katana" -> "Facebook"
            "com.instagram.android" -> "Instagram"
            "com.twitter.android" -> "Twitter"
            "com.whatsapp" -> "WhatsApp"
            "com.snapchat.android" -> "Snapchat"
            "com.zhiliaoapp.musically" -> "TikTok"
            "com.spotify.music" -> "Spotify"
            "com.netflix.mediaclient" -> "Netflix"
            "com.amazon.mShop.android.shopping" -> "Amazon"
            "com.google.android.apps.maps" -> "Google Maps"
            "com.microsoft.office.word" -> "Word"
            "com.microsoft.office.excel" -> "Excel"
            "com.microsoft.office.powerpoint" -> "PowerPoint"
            "com.slack" -> "Slack"
            "com.discord" -> "Discord"
            "com.telegram.messenger" -> "Telegram"
            "org.mozilla.firefox" -> "Firefox"
            "com.brave.browser" -> "Brave"
            "com.opera.browser" -> "Opera"
            "com.android.vending" -> "Play Store"
            "com.google.android.apps.photos" -> "Photos"
            "com.google.android.calendar" -> "Calendar"
            "com.google.android.keep" -> "Keep"
            "com.google.android.apps.docs" -> "Google Docs"
            "com.reddit.frontpage" -> "Reddit"
            "com.pinterest" -> "Pinterest"
            "com.linkedin.android" -> "LinkedIn"
            "com.microsoft.teams" -> "Teams"
            "com.zoom.videomeetings" -> "Zoom"
            "com.google.android.apps.meetings" -> "Google Meet"
            else -> null
        }
    }

    /**
     * Convert category ID to name
     */
    private fun getCategoryName(categoryId: Int): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (categoryId) {
                ApplicationInfo.CATEGORY_GAME -> "Game"
                ApplicationInfo.CATEGORY_AUDIO -> "Audio"
                ApplicationInfo.CATEGORY_VIDEO -> "Video"
                ApplicationInfo.CATEGORY_IMAGE -> "Image"
                ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                ApplicationInfo.CATEGORY_NEWS -> "News"
                ApplicationInfo.CATEGORY_MAPS -> "Maps"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                else -> "Other"
            }
        } else {
            "Other"
        }
    }

    /**
     * Determine if an app is productive
     */
    private fun isProductiveApp(packageName: String, category: String?): Boolean {
        // Productive categories
        val productiveCategories = setOf("Productivity", "Maps", "News")
        if (category in productiveCategories) return true

        // Productive apps by package name
        val productivePackages = setOf(
            "com.google.android.calendar",
            "com.microsoft.office",
            "com.google.android.apps.docs",
            "com.evernote",
            "com.todoist",
            "com.any.do",
            "com.google.android.keep",
            "com.notion",
            "com.trello",
            "com.asana",
            "com.slack",
            "com.microsoft.teams",
            "com.zoom.videomeetings",
            "com.google.android.apps.meetings",
            "org.mozilla.firefox",
            "com.android.chrome" // Can be productive or distracting
        )
        if (packageName in productivePackages) return true

        // Distracting categories
        val distractingCategories = setOf("Game", "Social", "Video", "Entertainment")
        if (category in distractingCategories) return false

        // Distracting apps by package name
        val distractingPackages = setOf(
            "com.facebook.katana",
            "com.instagram.android",
            "com.twitter.android",
            "com.snapchat.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "com.spotify.music",
            "com.reddit.frontpage",
            "com.pinterest"
        )
        
        return packageName !in distractingPackages
    }

    /**
     * Check if an app is a system app
     */
    private fun isSystemApp(packageName: String): Boolean {
        val systemPackages = setOf(
            "android",
            "com.android.systemui",
            "com.android.launcher",
            "com.google.android.gms",
            "com.google.android.gsf"
        )
        return systemPackages.any { packageName.startsWith(it) }
    }

    data class AppInfo(
        val name: String,
        val category: String
    )
}

/**
 * App usage data model
 */
data class AppUsageData(
    val packageName: String,
    val appName: String,
    val usageTimeMinutes: Int,
    val category: String,
    val isProductive: Boolean,
    val timestamp: Long
)

/**
 * Daily summary model
 */
data class DailySummary(
    val timestamp: Long,
    val totalScreenTimeMinutes: Int,
    val unlockCount: Int,
    val distractingAppTimeMinutes: Int,
    val productiveAppTimeMinutes: Int,
    val peakUsageHour: Int
)

