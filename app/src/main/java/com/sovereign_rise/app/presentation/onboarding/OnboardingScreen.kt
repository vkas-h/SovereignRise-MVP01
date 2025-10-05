package com.sovereign_rise.app.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sovereign_rise.app.presentation.components.GradientButton
import com.sovereign_rise.app.presentation.navigation.Screen
import com.sovereign_rise.app.presentation.viewmodel.OnboardingViewModel
import com.sovereign_rise.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.2f),
                        BackgroundPrimary,
                        BackgroundSecondary
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.default),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    viewModel.skipOnboarding()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }) {
                    Text("Skip", style = Typography.bodyMedium, color = TextSecondary)
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        icon = Icons.Outlined.Home,
                        title = "Welcome to Sovereign Rise",
                        subtitle = "A self-mastery journey through tasks, habits, and discipline."
                    )
                    1 -> OnboardingPage(
                        icon = Icons.Outlined.TrendingUp,
                        title = "Level Up Your Life",
                        subtitle = "Earn XP, unlock artifacts, and evolve your character."
                    )
                    2 -> OnboardingPage(
                        icon = Icons.Outlined.Verified,
                        title = "Powered by Honesty",
                        subtitle = "Progress only counts if you're truthful with yourself."
                    )
                    3 -> OnboardingPage(
                        icon = Icons.Outlined.Group,
                        title = "Rise Together",
                        subtitle = "Join guilds, find accountability partners, achieve more."
                    )
                    4 -> OnboardingPage(
                        icon = Icons.Outlined.Rocket,
                        title = "Your Journey Awaits",
                        subtitle = "Sign up and take your first step toward sovereignty.",
                        isLast = true,
                        onGetStarted = {
                            viewModel.completeOnboarding()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
            }

            // Page indicators
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Primary else TextMuted.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedVisibility(visible = pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Text("Back", color = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (pagerState.currentPage < 4) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLast: Boolean = false,
    onGetStarted: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(96.dp)
        )

        Spacer(modifier = Modifier.height(Spacing.massive))

        Text(
            text = title,
            style = Typography.headlineLarge,
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.default))

        Text(
            text = subtitle,
            style = Typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        if (isLast) {
            Spacer(modifier = Modifier.height(Spacing.massive))
            GradientButton(
                text = "Get Started",
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

