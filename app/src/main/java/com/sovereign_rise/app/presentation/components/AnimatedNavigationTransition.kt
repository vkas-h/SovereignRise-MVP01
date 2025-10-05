package com.sovereign_rise.app.presentation.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

// Animation duration
private const val ANIMATION_DURATION = 300

/**
 * Slide in from right animation for forward navigation
 */
val slideInFromRight: EnterTransition = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
)

/**
 * Slide out to left animation for forward navigation
 */
val slideOutToLeft: ExitTransition = slideOutHorizontally(
    targetOffsetX = { -it },
    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
)

/**
 * Slide in from left animation for backward navigation
 */
val slideInFromLeft: EnterTransition = slideInHorizontally(
    initialOffsetX = { -it },
    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
)

/**
 * Slide out to right animation for backward navigation
 */
val slideOutToRight: ExitTransition = slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
)

/**
 * Fade in animation
 */
val fadeInTransition: EnterTransition = fadeIn(
    animationSpec = tween(ANIMATION_DURATION)
)

/**
 * Fade out animation
 */
val fadeOutTransition: ExitTransition = fadeOut(
    animationSpec = tween(ANIMATION_DURATION)
)

/**
 * Extension function for composable with default slide transitions
 */
fun NavGraphBuilder.composableWithTransition(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = { slideInFromRight },
        exitTransition = { slideOutToLeft },
        popEnterTransition = { slideInFromLeft },
        popExitTransition = { slideOutToRight }
    ) {
        content(it)
    }
}

/**
 * Extension function for composable with fade transitions
 */
fun NavGraphBuilder.composableWithFade(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = { fadeInTransition },
        exitTransition = { fadeOutTransition },
        popEnterTransition = { fadeInTransition },
        popExitTransition = { fadeOutTransition }
    ) {
        content(it)
    }
}

