package com.sovereign_rise.app.util

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Performance optimization utilities for smooth UI.
 */

/**
 * Optimized modifier for list items to reduce recomposition.
 * Use this on LazyColumn items for better performance.
 */
fun Modifier.optimizedListItem(): Modifier = this.graphicsLayer {
    // Force layer creation to enable hardware acceleration
    // This prevents expensive recompositions from affecting parent
}

/**
 * Remember a value that depends on multiple keys.
 * More efficient than nested remember calls.
 */
@Composable
fun <T> rememberMultiKey(vararg keys: Any?, calculation: () -> T): T {
    return remember(*keys) { calculation() }
}

/**
 * Derived state that only recomputes when dependencies change.
 * Use for expensive calculations in Composables.
 */
@Composable
fun <T> rememberDerived(vararg keys: Any?, calculation: () -> T): State<T> {
    return remember(*keys) { derivedStateOf { calculation() } }
}

/**
 * Stable wrapper for lambda callbacks to prevent recomposition.
 */
@Composable
fun <T> rememberStableCallback(callback: (T) -> Unit): (T) -> Unit {
    val currentCallback by rememberUpdatedState(callback)
    return remember {
        { value: T -> currentCallback(value) }
    }
}

