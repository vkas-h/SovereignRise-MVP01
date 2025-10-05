package com.sovereign_rise.app.presentation.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Type of Snackbar message
 */
enum class SnackbarType {
    SUCCESS,
    ERROR,
    INFO,
    WARNING
}

/**
 * Data class representing a Snackbar message
 */
data class SnackbarMessage(
    val message: String,
    val type: SnackbarType,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

/**
 * Centralized Snackbar controller for consistent error/success message display
 */
object SnackbarController {
    private val _snackbarMessage = MutableStateFlow<SnackbarMessage?>(null)
    val snackbarMessage: StateFlow<SnackbarMessage?> = _snackbarMessage.asStateFlow()

    /**
     * Show a success message
     */
    fun showSuccess(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        _snackbarMessage.value = SnackbarMessage(
            message = message,
            type = SnackbarType.SUCCESS,
            duration = duration
        )
    }

    /**
     * Show an error message
     */
    fun showError(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Long,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        _snackbarMessage.value = SnackbarMessage(
            message = message,
            type = SnackbarType.ERROR,
            duration = duration,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }

    /**
     * Show an info message
     */
    fun showInfo(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        _snackbarMessage.value = SnackbarMessage(
            message = message,
            type = SnackbarType.INFO,
            duration = duration
        )
    }

    /**
     * Show a warning message
     */
    fun showWarning(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        _snackbarMessage.value = SnackbarMessage(
            message = message,
            type = SnackbarType.WARNING,
            duration = duration
        )
    }

    /**
     * Dismiss current message
     */
    fun dismiss() {
        _snackbarMessage.value = null
    }
}

/**
 * Composable function to handle Snackbar display
 */
@Composable
fun ObserveSnackbarMessages(snackbarHostState: SnackbarHostState) {
    val snackbarMessage by SnackbarController.snackbarMessage.collectAsState()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message.message,
                actionLabel = message.actionLabel,
                duration = message.duration
            )
            
            // Handle action if clicked
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                message.onAction?.invoke()
            }
            
            // Clear message after showing
            SnackbarController.dismiss()
        }
    }
}

