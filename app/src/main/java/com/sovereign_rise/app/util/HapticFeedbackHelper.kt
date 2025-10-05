package com.sovereign_rise.app.util

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay

object HapticFeedbackHelper {

    /**
     * Light tap for button presses and minor interactions
     */
    fun performLightTap(hapticFeedback: HapticFeedback) {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (e: Exception) {
            // Ignore haptic errors
        }
    }

    /**
     * Medium tap for selections and swipes
     */
    fun performMediumTap(hapticFeedback: HapticFeedback) {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            // Ignore haptic errors
        }
    }

    /**
     * Heavy tap for important actions
     */
    suspend fun performHeavyTap(hapticFeedback: HapticFeedback) {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(50)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            // Ignore haptic errors
        }
    }

    /**
     * Success pattern for task completions
     */
    suspend fun performSuccess(hapticFeedback: HapticFeedback) {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(50)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            // Ignore haptic errors
        }
    }

    /**
     * Error pattern for failures
     */
    suspend fun performError(hapticFeedback: HapticFeedback) {
        try {
            repeat(3) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(30)
            }
        } catch (e: Exception) {
            // Ignore haptic errors
        }
    }

    /**
     * Celebration pattern for milestones and level ups
     */
    suspend fun performCelebration(hapticFeedback: HapticFeedback) {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(100)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(100)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            // Ignore haptic errors
        }
    }
}

