package com.sovereign_rise.app.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.sovereign_rise.app.presentation.auth.LoginScreen
import com.sovereign_rise.app.presentation.auth.SignupScreen
import com.sovereign_rise.app.presentation.auth.UpgradeAccountScreen
import com.sovereign_rise.app.presentation.components.BottomNavigationBar
import com.sovereign_rise.app.presentation.profile.ProfileScreen
import com.sovereign_rise.app.presentation.profile.ProfileEditScreen
import com.sovereign_rise.app.presentation.splash.SplashScreen
import com.sovereign_rise.app.presentation.task.TaskListScreen
import com.sovereign_rise.app.presentation.task.AddEditTaskScreen
import com.sovereign_rise.app.presentation.habit.HabitListScreen
import com.sovereign_rise.app.presentation.habit.AddEditHabitScreen
import com.sovereign_rise.app.presentation.home.HomeScreen
import com.sovereign_rise.app.presentation.screens.OfflineModeSettingsScreen
import com.sovereign_rise.app.presentation.viewmodel.AuthViewModel
import com.sovereign_rise.app.presentation.viewmodel.ProfileViewModel
import com.sovereign_rise.app.presentation.viewmodel.TaskViewModel
import com.sovereign_rise.app.presentation.viewmodel.HabitViewModel
import com.sovereign_rise.app.presentation.viewmodel.HomeViewModel

/**
 * Navigation graph for the app.
 * Wires up all screens and navigation destinations.
 * 
 * @param navController Navigation controller for managing navigation
 * @param authViewModel ViewModel for authentication screens
 * @param profileViewModel ViewModel for profile screen
 * @param startDestination Initial destination (defaults to Login screen)
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    taskViewModel: TaskViewModel,
    habitViewModel: HabitViewModel,
    homeViewModel: HomeViewModel,
    aiFeaturesViewModel: com.sovereign_rise.app.presentation.viewmodel.AIFeaturesViewModel,
    analyticsViewModel: com.sovereign_rise.app.presentation.viewmodel.AnalyticsViewModel,
    onboardingViewModel: com.sovereign_rise.app.presentation.viewmodel.OnboardingViewModel,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash screen
        composable(Screen.Splash.route) {
            SplashScreen(
                navController = navController,
                viewModel = authViewModel,
                onboardingViewModel = onboardingViewModel
            )
        }
        
        // Onboarding screen
        composable(Screen.Onboarding.route) {
            com.sovereign_rise.app.presentation.onboarding.OnboardingScreen(
                navController = navController,
                viewModel = onboardingViewModel
            )
        }
        
        // Authentication screens
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }
        
        composable(Screen.Signup.route) {
            SignupScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }
        
        composable(Screen.UpgradeAccount.route) {
            UpgradeAccountScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }
        
        // Main app screens (with bottom navigation)
        composable(Screen.Home.route) {
            MainScreenWithBottomNav(
                navController = navController,
                homeViewModel = homeViewModel,
                taskViewModel = taskViewModel,
                habitViewModel = habitViewModel,
                profileViewModel = profileViewModel
            )
        }
        
        composable(Screen.Tasks.route) {
            MainScreenWithBottomNav(
                navController = navController,
                homeViewModel = homeViewModel,
                taskViewModel = taskViewModel,
                habitViewModel = habitViewModel,
                profileViewModel = profileViewModel
            )
        }
        
        composable(Screen.Habits.route) {
            MainScreenWithBottomNav(
                navController = navController,
                homeViewModel = homeViewModel,
                taskViewModel = taskViewModel,
                habitViewModel = habitViewModel,
                profileViewModel = profileViewModel
            )
        }
        
        composable(Screen.Profile.route) {
            MainScreenWithBottomNav(
                navController = navController,
                homeViewModel = homeViewModel,
                taskViewModel = taskViewModel,
                habitViewModel = habitViewModel,
                profileViewModel = profileViewModel
            )
        }
        
        composable(Screen.ProfileEdit.route) {
            ProfileEditScreen(
                navController = navController,
                viewModel = profileViewModel
            )
        }
        
        composable(Screen.AddTask.route) {
            AddEditTaskScreen(
                navController = navController,
                viewModel = taskViewModel,
                taskId = null
            )
        }
        
        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            AddEditTaskScreen(
                navController = navController,
                viewModel = taskViewModel,
                taskId = taskId
            )
        }
        
        composable(Screen.AddHabit.route) {
            AddEditHabitScreen(
                navController = navController,
                viewModel = habitViewModel,
                habitId = null
            )
        }
        
        composable(
            route = Screen.EditHabit.route,
            arguments = listOf(navArgument("habitId") { type = NavType.StringType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getString("habitId")
            AddEditHabitScreen(
                navController = navController,
                viewModel = habitViewModel,
                habitId = habitId
            )
        }
        
        composable(Screen.AIFeaturesSettings.route) {
            com.sovereign_rise.app.presentation.settings.AIFeaturesSettingsScreen(
                navController = navController,
                viewModel = aiFeaturesViewModel
            )
        }
        
        composable(Screen.Analytics.route) {
            com.sovereign_rise.app.presentation.analytics.AnalyticsScreen(
                navController = navController,
                viewModel = analyticsViewModel
            )
        }
        
        composable(Screen.OfflineModeSettings.route) {
            OfflineModeSettingsScreen(
                taskViewModel = taskViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.UsageAnalytics.route) {
            PlaceholderScreen(title = "Usage Analytics (Coming Soon)")
        }
        
        composable(Screen.Market.route) {
            // TODO: Implement level check for rare tier (level 20)
            PlaceholderScreen(title = "Market (Rare tier unlocks at Level 20)")
        }
        
        composable(Screen.Guilds.route) {
            // TODO: Implement level check and show FeatureLockedOverlay if level < 5
            PlaceholderScreen(title = "Guilds (Unlocks at Level 5)")
        }
    }
}

/**
 * Main screen wrapper with bottom navigation bar.
 * Displays the appropriate screen based on current route.
 */
@Composable
private fun MainScreenWithBottomNav(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    taskViewModel: TaskViewModel,
    habitViewModel: HabitViewModel,
    profileViewModel: ProfileViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentRoute) {
                Screen.Home.route -> {
                    HomeScreen(
                        navController = navController,
                        viewModel = homeViewModel,
                        taskViewModel = taskViewModel
                    )
                }
                Screen.Tasks.route -> {
                    TaskListScreen(
                        navController = navController,
                        viewModel = taskViewModel
                    )
                }
                Screen.Habits.route -> {
                    HabitListScreen(
                        navController = navController,
                        viewModel = habitViewModel
                    )
                }
                Screen.Profile.route -> {
                    ProfileScreen(
                        navController = navController,
                        viewModel = profileViewModel
                    )
                }
                else -> {
                    // Default to Home if route doesn't match
                    HomeScreen(
                        navController = navController,
                        viewModel = homeViewModel,
                        taskViewModel = taskViewModel
                    )
                }
            }
        }
    }
}

/**
 * Placeholder screen for unimplemented features.
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Coming soon...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}