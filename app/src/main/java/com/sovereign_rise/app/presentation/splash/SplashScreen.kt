package com.sovereign_rise.app.presentation.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sovereign_rise.app.presentation.navigation.Screen
import com.sovereign_rise.app.presentation.viewmodel.AuthUiState
import com.sovereign_rise.app.presentation.viewmodel.AuthViewModel
import com.sovereign_rise.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Splash screen that checks authentication status on app startup
 */
@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    onboardingViewModel: com.sovereign_rise.app.presentation.viewmodel.OnboardingViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val onboardingState by onboardingViewModel.uiState.collectAsState()
    var shouldNavigate by remember { mutableStateOf(false) }

    // Animated fade-in effect
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "splash_fade_in"
    )

    // Check auth status and wait for minimum display time
    LaunchedEffect(Unit) {
        viewModel.checkAuthStatus()
        
        // Minimum display time for branding
        delay(1000)
        shouldNavigate = true
    }

    // Navigate based on onboarding and auth state
    LaunchedEffect(onboardingState, uiState, shouldNavigate) {
        if (shouldNavigate) {
            // Priority 1: If onboarding not completed, go to onboarding
            if (onboardingState is com.sovereign_rise.app.presentation.viewmodel.OnboardingUiState.OnboardingInProgress) {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
                return@LaunchedEffect
            }
            
            // Priority 2: Check auth status
            when (uiState) {
                is AuthUiState.Success -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                is AuthUiState.GuestMode -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                is AuthUiState.Idle, is AuthUiState.Error -> {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                else -> {
                    // Still loading, wait
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BackgroundPrimary,
                        Primary.copy(alpha = 0.2f),
                        Secondary.copy(alpha = 0.2f),
                        BackgroundSecondary
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo/title with modern typography
            Text(
                text = "Sovereign Rise",
                style = Typography.displayLarge,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            // Subtitle
            Text(
                text = "Master yourself, rise above",
                style = Typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.massive))

            // Loading indicator
            CircularProgressIndicator(
                color = Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
        }

        // Bottom text
        Text(
            text = "Made by VKAS || MVP 01",
            style = Typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.extraLarge)
                .fillMaxWidth()
        )
    }
}

