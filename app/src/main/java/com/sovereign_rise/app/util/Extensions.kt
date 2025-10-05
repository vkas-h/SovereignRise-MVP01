package com.sovereign_rise.app.util

import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Date/Time Extension Functions
 */

/**
 * Formats timestamp to "MMM dd, yyyy"
 */
fun Long.toFormattedDate(): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(this))
}

/**
 * Formats timestamp to "h:mm a"
 */
fun Long.toFormattedTime(): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(this))
}

/**
 * Formats timestamp to "MMM dd, yyyy 'at' h:mm a"
 */
fun Long.toFormattedDateTime(): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(Date(this))
}

/**
 * Formats timestamp to relative time
 */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = this - now
    
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    
    return when {
        diff < 0 -> {
            val absDiff = -diff
            val absMinutes = TimeUnit.MILLISECONDS.toMinutes(absDiff)
            val absHours = TimeUnit.MILLISECONDS.toHours(absDiff)
            val absDays = TimeUnit.MILLISECONDS.toDays(absDiff)
            
            when {
                absMinutes < 1 -> "Just now"
                absMinutes < 60 -> "$absMinutes minute${if (absMinutes == 1L) "" else "s"} ago"
                absHours < 24 -> "$absHours hour${if (absHours == 1L) "" else "s"} ago"
                absDays == 1L -> "Yesterday"
                absDays < 7 -> "$absDays days ago"
                else -> this.toFormattedDate()
            }
        }
        days == 0L && hours < 24 -> {
            when {
                minutes < 60 -> "In $minutes minute${if (minutes == 1L) "" else "s"}"
                hours < 24 -> "In $hours hour${if (hours == 1L) "" else "s"}"
                else -> "Today at ${this.toFormattedTime()}"
            }
        }
        days == 1L -> "Tomorrow at ${this.toFormattedTime()}"
        days < 7 -> "In $days days at ${this.toFormattedTime()}"
        else -> this.toFormattedDateTime()
    }
}

/**
 * Checks if timestamp is today (same UTC day)
 */
fun Long.isToday(): Boolean {
    val calendar1 = Calendar.getInstance().apply {
        timeInMillis = this@isToday
    }
    val calendar2 = Calendar.getInstance()
    
    return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
            calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
}

/**
 * Checks if two timestamps are on the same day
 */
fun Long.isSameDay(other: Long): Boolean {
    val calendar1 = Calendar.getInstance().apply {
        timeInMillis = this@isSameDay
    }
    val calendar2 = Calendar.getInstance().apply {
        timeInMillis = other
    }
    
    return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
            calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
}

/**
 * String Extension Functions
 */

/**
 * Validates email format
 */
fun String.isValidEmail(): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return emailRegex.matches(this)
}

/**
 * Validates username format (3-20 chars, alphanumeric + underscore)
 */
fun String.isValidUsername(): Boolean {
    val usernameRegex = "^[a-zA-Z0-9_]{3,20}$".toRegex()
    return usernameRegex.matches(this)
}

/**
 * Truncates string with ellipsis
 */
fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
    return if (length <= maxLength) {
        this
    } else {
        take(maxLength - ellipsis.length) + ellipsis
    }
}

/**
 * Number Extension Functions
 */

/**
 * Formats numbers with thousand separators
 */
fun Int.formatWithCommas(): String {
    return String.format(Locale.getDefault(), "%,d", this)
}

/**
 * Formats float as percentage
 */
fun Float.toPercentage(): String {
    return "${(this * 100).toInt()}%"
}

/**
 * Color Extension Functions
 */

/**
 * Converts hex string to Compose Color
 */
fun String.toComposeColor(): Color {
    val cleanHex = removePrefix("#")
    val colorLong = cleanHex.toLongOrNull(16) ?: 0
    return Color(colorLong or 0xFF000000)
}

/**
 * Navigation Extension Functions
 */

/**
 * Navigates and clears entire backstack
 */
fun NavController.navigateAndClearBackStack(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * Navigates with singleTop and restoreState
 */
fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}

