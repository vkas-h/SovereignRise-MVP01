package com.sovereign_rise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.sovereign_rise.app.di.AppModule
import com.sovereign_rise.app.presentation.navigation.NavGraph
import com.sovereign_rise.app.ui.theme.BackgroundDark
import com.sovereign_rise.app.ui.theme.BackgroundLight
import com.sovereign_rise.app.ui.theme.SovereignRiseTheme
import com.sovereign_rise.app.util.ConnectivityStatus
import com.sovereign_rise.app.worker.CacheCleanupWorker
import com.sovereign_rise.app.worker.SyncWorker
import kotlinx.coroutines.launch

/**
 * Main activity that serves as the entry point for the app.
 * Sets up the navigation graph and provides ViewModels via manual DI.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Provide ViewModels using AppModule (manual DI)
        // TODO: Replace with Hilt dependency injection in future phases
        val authViewModel = AppModule.provideAuthViewModel(applicationContext)
        val profileViewModel = AppModule.provideProfileViewModel(applicationContext)
        val taskViewModel = AppModule.provideTaskViewModel(applicationContext)
        val habitViewModel = AppModule.provideHabitViewModel(applicationContext)
        val homeViewModel = AppModule.provideHomeViewModel(applicationContext)
        val aiFeaturesViewModel = AppModule.provideAIFeaturesViewModel(applicationContext)
        val analyticsViewModel = AppModule.provideAnalyticsViewModel(applicationContext)
        val onboardingViewModel = AppModule.provideOnboardingViewModel(applicationContext)
        
        // Initialize AI features
        com.sovereign_rise.app.util.NotificationHelper.createNotificationChannels(this)
        com.sovereign_rise.app.worker.UsageStatsWorker.enqueueWork(this)
        com.sovereign_rise.app.worker.BurnoutCheckWorker.enqueueWork(this)
        com.sovereign_rise.app.worker.UsageNudgeWorker.enqueueWork(this)
        
        // Initialize offline mode & sync engine
        SyncWorker.enqueueWork(this)
        CacheCleanupWorker.enqueueWork(this)
        
        // Observe connectivity and trigger sync when online
        val connectivityObserver = AppModule.provideConnectivityObserver(applicationContext)
        val syncManager = AppModule.provideSyncManager(applicationContext)
        
        lifecycleScope.launch {
            connectivityObserver.observe().collect { status ->
                if (status == ConnectivityStatus.AVAILABLE) {
                    // Trigger immediate sync when connectivity returns
                    SyncWorker.enqueueSyncNow(applicationContext)
                }
            }
        }
        
        // On app startup, check if there are pending sync actions
        lifecycleScope.launch {
            val pendingCount = syncManager.getPendingSyncCount()
            if (pendingCount > 0 && syncManager.isOnline()) {
                // Trigger immediate sync for pending actions
                SyncWorker.enqueueSyncNow(applicationContext)
            }
        }
        
        setContent {
            SovereignRiseTheme {
                // Configure system UI (status bar and navigation bar)
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    // Status bar
                    systemUiController.setStatusBarColor(
                        color = BackgroundDark,
                        darkIcons = false
                    )
                    // Navigation bar
                    systemUiController.setNavigationBarColor(
                        color = BackgroundLight,
                        darkIcons = false
                    )
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        authViewModel = authViewModel,
                        profileViewModel = profileViewModel,
                        taskViewModel = taskViewModel,
                        habitViewModel = habitViewModel,
                        homeViewModel = homeViewModel,
                        aiFeaturesViewModel = aiFeaturesViewModel,
                        analyticsViewModel = analyticsViewModel,
                        onboardingViewModel = onboardingViewModel
                    )
                }
            }
        }
    }
}