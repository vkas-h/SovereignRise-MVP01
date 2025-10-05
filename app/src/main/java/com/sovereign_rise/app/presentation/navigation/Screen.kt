package com.sovereign_rise.app.presentation.navigation

sealed class Screen(val route: String) {
    
    // Splash screen
    data object Splash : Screen("splash")
    
    // Onboarding
    data object Onboarding : Screen("onboarding")
    
    // Authentication flow
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object UpgradeAccount : Screen("upgrade_account")
    
    // Main app screens
    data object Home : Screen("home")
    data object Tasks : Screen("tasks")
    data object Profile : Screen("profile")
    data object ProfileEdit : Screen("profile/edit")
    data object Market : Screen("market")
    data object Guilds : Screen("guilds")
    
    // Task screens
    data object AddTask : Screen("tasks/add")
    data object EditTask : Screen("tasks/edit/{taskId}") {
        fun createRoute(taskId: String) = "tasks/edit/$taskId"
    }
    
    // Habit screens
    data object Habits : Screen("habits")
    data object AddHabit : Screen("habits/add")
    data object EditHabit : Screen("habits/edit/{habitId}") {
        fun createRoute(habitId: String) = "habits/edit/$habitId"
    }
    
    // AI Features screens
    data object AIFeaturesSettings : Screen("ai_features_settings")
    data object UsageAnalytics : Screen("usage_analytics")
    
    // Analytics
    data object Analytics : Screen("analytics")
    
    // Offline Mode settings
    data object OfflineModeSettings : Screen("offline_mode_settings")
    
    companion object {
        /**
         * Returns all main navigation destinations (excludes auth screens).
         */
        fun mainScreens(): List<Screen> = listOf(
            Home,
            Tasks,
            Profile,
            Market,
            Guilds
        )
    }
}
