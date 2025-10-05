package com.sovereign_rise.app.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.sovereign_rise.app.data.local.TokenDataStore
import com.sovereign_rise.app.data.remote.NetworkModule
import com.sovereign_rise.app.data.remote.api.AuthApiService
import com.sovereign_rise.app.data.remote.api.TaskApiService
import com.sovereign_rise.app.data.remote.api.HabitApiService
import com.sovereign_rise.app.data.remote.api.AIApiService
import com.sovereign_rise.app.data.repository.AuthRepositoryImpl
import com.sovereign_rise.app.data.repository.TaskRepositoryImpl
import com.sovereign_rise.app.data.repository.HabitRepositoryImpl
import com.sovereign_rise.app.domain.repository.AuthRepository
import com.sovereign_rise.app.domain.repository.TaskRepository
import com.sovereign_rise.app.domain.repository.HabitRepository
import com.sovereign_rise.app.domain.usecase.auth.*
import com.sovereign_rise.app.domain.usecase.task.*
import com.sovereign_rise.app.domain.usecase.habit.*
import com.sovereign_rise.app.domain.usecase.user.*
import com.sovereign_rise.app.presentation.viewmodel.AuthViewModel
import com.sovereign_rise.app.presentation.viewmodel.ProfileViewModel
import com.sovereign_rise.app.presentation.viewmodel.TaskViewModel
import com.sovereign_rise.app.presentation.viewmodel.HabitViewModel
import com.sovereign_rise.app.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.runBlocking

/**
 * Manual dependency injection module.
 * Provides singleton instances of repositories, use cases, and view models.
 * 
 * TODO: Replace with Hilt dependency injection in future phases for better scalability.
 */
object AppModule {
    
    // Singletons
    @Volatile
    private var authRepository: AuthRepository? = null
    
    @Volatile
    private var authViewModel: AuthViewModel? = null
    
    @Volatile
    private var profileViewModel: ProfileViewModel? = null
    
    @Volatile
    private var taskRepository: TaskRepository? = null
    
    @Volatile
    private var taskViewModel: TaskViewModel? = null
    
    @Volatile
    private var habitRepository: HabitRepository? = null
    
    @Volatile
    private var habitViewModel: HabitViewModel? = null
    
    @Volatile
    private var homeViewModel: HomeViewModel? = null
    
    @Volatile
    private var usageStatsRepository: com.sovereign_rise.app.domain.repository.UsageStatsRepository? = null
    
    @Volatile
    private var smartReminderRepository: com.sovereign_rise.app.domain.repository.SmartReminderRepository? = null
    
    @Volatile
    private var burnoutRepository: com.sovereign_rise.app.domain.repository.BurnoutRepository? = null
    
    @Volatile
    private var affirmationRepository: com.sovereign_rise.app.domain.repository.AffirmationRepository? = null
    
    @Volatile
    private var aiFeaturesViewModel: com.sovereign_rise.app.presentation.viewmodel.AIFeaturesViewModel? = null
    
    // Offline mode singletons
    @Volatile
    private var appDatabase: com.sovereign_rise.app.data.local.UsageStatsDatabase? = null
    
    @Volatile
    private var syncManager: com.sovereign_rise.app.data.sync.SyncManager? = null
    
    @Volatile
    private var connectivityObserver: com.sovereign_rise.app.util.ConnectivityObserver? = null
    
    @Volatile
    private var userRepository: com.sovereign_rise.app.domain.repository.UserRepository? = null
    
    @Volatile
    private var onboardingRepository: com.sovereign_rise.app.domain.repository.OnboardingRepository? = null
    
    @Volatile
    private var onboardingViewModel: com.sovereign_rise.app.presentation.viewmodel.OnboardingViewModel? = null
    
    @Volatile
    private var analyticsRepository: com.sovereign_rise.app.domain.repository.AnalyticsRepository? = null
    
    @Volatile
    private var analyticsViewModel: com.sovereign_rise.app.presentation.viewmodel.AnalyticsViewModel? = null
    
    /**
     * Provides Firebase Auth instance.
     */
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    /**
     * Provides TokenDataStore instance.
     */
    fun provideTokenDataStore(context: Context): TokenDataStore {
        return TokenDataStore(context)
    }
    
    /**
     * Provides AuthApiService instance.
     */
    fun provideAuthApiService(): AuthApiService {
        return NetworkModule.createAuthApiService()
    }
    
    /**
     * Provides AuthRepository instance (singleton).
     * Seeds the cached token from TokenDataStore on startup.
     */
    fun provideAuthRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(this) {
            authRepository ?: AuthRepositoryImpl(
                firebaseAuth = provideFirebaseAuth(),
                authApiService = provideAuthApiService(),
                tokenDataStore = provideTokenDataStore(context),
                context = context.applicationContext,
                userDao = provideUserDao(context),
                connectivityObserver = provideConnectivityObserver(context)
            ).also { 
                authRepository = it
                
                // Seed the cached token from TokenDataStore on startup
                runBlocking {
                    val tokenDataStore = provideTokenDataStore(context)
                    val cachedToken = tokenDataStore.getToken()
                    if (cachedToken != null && tokenDataStore.isTokenValid()) {
                        NetworkModule.setAuthToken(cachedToken)
                    }
                }
            }
        }
    }
    
    /**
     * Provides LoginUseCase instance.
     */
    fun provideLoginUseCase(context: Context, authRepository: AuthRepository): LoginUseCase {
        return LoginUseCase(authRepository, provideConnectivityObserver(context))
    }
    
    /**
     * Provides RegisterUseCase instance.
     */
    fun provideRegisterUseCase(context: Context, authRepository: AuthRepository): RegisterUseCase {
        return RegisterUseCase(authRepository, provideConnectivityObserver(context))
    }
    
    /**
     * Provides GoogleSignInUseCase instance.
     */
    fun provideGoogleSignInUseCase(authRepository: AuthRepository): GoogleSignInUseCase {
        return GoogleSignInUseCase(authRepository)
    }
    
    /**
     * Provides GuestLoginUseCase instance.
     */
    fun provideGuestLoginUseCase(context: Context, authRepository: AuthRepository): GuestLoginUseCase {
        return GuestLoginUseCase(authRepository, provideConnectivityObserver(context))
    }
    
    /**
     * Provides UpgradeAccountUseCase instance.
     */
    fun provideUpgradeAccountUseCase(authRepository: AuthRepository): UpgradeAccountUseCase {
        return UpgradeAccountUseCase(authRepository)
    }
    
    /**
     * Provides LogoutUseCase instance.
     */
    fun provideLogoutUseCase(authRepository: AuthRepository): LogoutUseCase {
        return LogoutUseCase(authRepository)
    }
    
    /**
     * Provides GetCurrentUserUseCase instance.
     */
    fun provideGetCurrentUserUseCase(authRepository: AuthRepository): GetCurrentUserUseCase {
        return GetCurrentUserUseCase(authRepository)
    }
    
    /**
     * Provides AuthViewModel instance (singleton per app session).
     * TODO: Use proper ViewModel factory for lifecycle-aware ViewModels.
     */
    fun provideAuthViewModel(context: Context): AuthViewModel {
        return authViewModel ?: synchronized(this) {
            val repository = provideAuthRepository(context)
            authViewModel ?: AuthViewModel(
                loginUseCase = provideLoginUseCase(context, repository),
                registerUseCase = provideRegisterUseCase(context, repository),
                googleSignInUseCase = provideGoogleSignInUseCase(repository),
                guestLoginUseCase = provideGuestLoginUseCase(context, repository),
                upgradeAccountUseCase = provideUpgradeAccountUseCase(repository),
                logoutUseCase = provideLogoutUseCase(repository),
                getCurrentUserUseCase = provideGetCurrentUserUseCase(repository),
                authRepository = repository
            ).also { authViewModel = it }
        }
    }
    
    /**
     * Provides ProfileViewModel instance (singleton per app session).
     * TODO: Use proper ViewModel factory for lifecycle-aware ViewModels.
     */
    fun provideProfileViewModel(context: Context): ProfileViewModel {
        return profileViewModel ?: synchronized(this) {
            val authRepository = provideAuthRepository(context)
            val userRepository = provideUserRepository(context)
            profileViewModel ?: ProfileViewModel(
                getCurrentUserUseCase = provideGetCurrentUserUseCase(authRepository),
                logoutUseCase = provideLogoutUseCase(authRepository),
                updateProfileUseCase = provideUpdateProfileUseCase(userRepository),
                authRepository = authRepository
            ).also { profileViewModel = it }
        }
    }
    
    /**
     * Provides UpdateProfileUseCase instance.
     */
    fun provideUpdateProfileUseCase(userRepository: com.sovereign_rise.app.domain.repository.UserRepository): UpdateProfileUseCase {
        return UpdateProfileUseCase(userRepository)
    }
    
    /**
     * Provides TaskApiService instance.
     */
    fun provideTaskApiService(): TaskApiService {
        return NetworkModule.createTaskApiService()
    }
    
    /**
     * Provides TaskRepository instance (singleton).
     */
    fun provideTaskRepository(context: Context): TaskRepository {
        return taskRepository ?: synchronized(this) {
            taskRepository ?: TaskRepositoryImpl(
                context = context,
                taskDao = provideTaskDao(context),
                taskApiService = provideTaskApiService(),
                syncManager = provideSyncManager(context),
                tokenDataStore = provideTokenDataStore(context)
            ).also { taskRepository = it }
        }
    }
    
    /**
     * Provides GetTasksUseCase instance.
     */
    fun provideGetTasksUseCase(taskRepository: TaskRepository): GetTasksUseCase {
        return GetTasksUseCase(taskRepository)
    }
    
    /**
     * Provides CreateTaskUseCase instance.
     */
    fun provideCreateTaskUseCase(taskRepository: TaskRepository): CreateTaskUseCase {
        return CreateTaskUseCase(taskRepository)
    }
    
    /**
     * Provides UpdateTaskUseCase instance.
     */
    fun provideUpdateTaskUseCase(taskRepository: TaskRepository): UpdateTaskUseCase {
        return UpdateTaskUseCase(taskRepository)
    }
    
    /**
     * Provides DeleteTaskUseCase instance.
     */
    fun provideDeleteTaskUseCase(taskRepository: TaskRepository): DeleteTaskUseCase {
        return DeleteTaskUseCase(taskRepository)
    }
    
    /**
     * Provides CompleteTaskUseCase instance.
     */
    fun provideCompleteTaskUseCase(taskRepository: TaskRepository): CompleteTaskUseCase {
        return CompleteTaskUseCase(taskRepository)
    }
    
    /**
     * Provides TriggerDailyResetUseCase instance.
     */
    fun provideTriggerDailyResetUseCase(taskRepository: TaskRepository): TriggerDailyResetUseCase {
        return TriggerDailyResetUseCase(taskRepository)
    }
    
    /**
     * Provides TaskViewModel instance (singleton per app session).
     */
    fun provideTaskViewModel(context: Context): TaskViewModel {
        return taskViewModel ?: synchronized(this) {
            val repository = provideTaskRepository(context)
            taskViewModel ?: TaskViewModel(
                getTasksUseCase = provideGetTasksUseCase(repository),
                createTaskUseCase = provideCreateTaskUseCase(repository),
                updateTaskUseCase = provideUpdateTaskUseCase(repository),
                deleteTaskUseCase = provideDeleteTaskUseCase(repository),
                completeTaskUseCase = provideCompleteTaskUseCase(repository),
                triggerDailyResetUseCase = provideTriggerDailyResetUseCase(repository),
                generateAffirmationUseCase = provideGenerateAffirmationUseCase(context),
                syncManager = provideSyncManager(context),
                connectivityObserver = provideConnectivityObserver(context),
                taskRepository = repository as com.sovereign_rise.app.data.repository.TaskRepositoryImpl
            ).also { taskViewModel = it }
        }
    }
    
    /**
     * Provides HabitApiService instance.
     */
    fun provideHabitApiService(): HabitApiService {
        return NetworkModule.createHabitApiService()
    }
    
    /**
     * Provides HabitRepository instance (singleton).
     */
    fun provideHabitRepository(context: Context): HabitRepository {
        return habitRepository ?: synchronized(this) {
            habitRepository ?: HabitRepositoryImpl(
                habitDao = provideHabitDao(context),
                habitApiService = provideHabitApiService(),
                syncManager = provideSyncManager(context),
                tokenDataStore = provideTokenDataStore(context)
            ).also { habitRepository = it }
        }
    }
    
    /**
     * Provides GetHabitsUseCase instance.
     */
    fun provideGetHabitsUseCase(habitRepository: HabitRepository): GetHabitsUseCase {
        return GetHabitsUseCase(habitRepository)
    }
    
    /**
     * Provides CreateHabitUseCase instance.
     */
    fun provideCreateHabitUseCase(habitRepository: HabitRepository): CreateHabitUseCase {
        return CreateHabitUseCase(habitRepository)
    }
    
    /**
     * Provides UpdateHabitUseCase instance.
     */
    fun provideUpdateHabitUseCase(habitRepository: HabitRepository): UpdateHabitUseCase {
        return UpdateHabitUseCase(habitRepository)
    }
    
    /**
     * Provides DeleteHabitUseCase instance.
     */
    fun provideDeleteHabitUseCase(habitRepository: HabitRepository): DeleteHabitUseCase {
        return DeleteHabitUseCase(habitRepository)
    }
    
    /**
     * Provides TickHabitUseCase instance.
     */
    fun provideTickHabitUseCase(habitRepository: HabitRepository): TickHabitUseCase {
        return TickHabitUseCase(habitRepository)
    }
    
    /**
     * Provides CheckStreakBreaksUseCase instance.
     */
    fun provideCheckStreakBreaksUseCase(habitRepository: HabitRepository): CheckStreakBreaksUseCase {
        return CheckStreakBreaksUseCase(habitRepository)
    }
    
    /**
     * Provides GetHabitByIdUseCase instance.
     */
    fun provideGetHabitByIdUseCase(habitRepository: HabitRepository): GetHabitByIdUseCase {
        return GetHabitByIdUseCase(habitRepository)
    }
    
    /**
     * Provides HabitViewModel instance (singleton per app session).
     */
    fun provideHabitViewModel(context: Context): HabitViewModel {
        return habitViewModel ?: synchronized(this) {
            val repository = provideHabitRepository(context)
            habitViewModel ?: HabitViewModel(
                getHabitsUseCase = provideGetHabitsUseCase(repository),
                createHabitUseCase = provideCreateHabitUseCase(repository),
                updateHabitUseCase = provideUpdateHabitUseCase(repository),
                deleteHabitUseCase = provideDeleteHabitUseCase(repository),
                tickHabitUseCase = provideTickHabitUseCase(repository),
                checkStreakBreaksUseCase = provideCheckStreakBreaksUseCase(repository),
                getHabitByIdUseCase = provideGetHabitByIdUseCase(repository),
                generateAffirmationUseCase = provideGenerateAffirmationUseCase(context),
                syncManager = provideSyncManager(context),
                connectivityObserver = provideConnectivityObserver(context),
                habitRepository = repository as com.sovereign_rise.app.data.repository.HabitRepositoryImpl
            ).also { habitViewModel = it }
        }
    }
    
    
    /**
     * Provides HomeViewModel instance (singleton per app session).
     */
    fun provideHomeViewModel(context: Context): HomeViewModel {
        return homeViewModel ?: synchronized(this) {
            val authRepository = provideAuthRepository(context)
            val taskRepository = provideTaskRepository(context)
            val habitRepository = provideHabitRepository(context)
            homeViewModel ?: HomeViewModel(
                getCurrentUserUseCase = provideGetCurrentUserUseCase(authRepository),
                getTasksUseCase = provideGetTasksUseCase(taskRepository),
                getHabitsUseCase = provideGetHabitsUseCase(habitRepository),
                getYesterdayTaskSummaryUseCase = com.sovereign_rise.app.domain.usecase.task.GetYesterdayTaskSummaryUseCase(provideTaskApiService())
            ).also { homeViewModel = it }
        }
    }
    
    // ============ AI Features Dependencies ============
    
    /**
     * Provides UsageStatsDatabase instance.
     */
    fun provideUsageStatsDatabase(context: Context): com.sovereign_rise.app.data.local.UsageStatsDatabase {
        return com.sovereign_rise.app.data.local.UsageStatsDatabase.getInstance(context)
    }
    
    /**
     * Provides UsageStatsApiService instance.
     */
    fun provideUsageStatsApiService(): com.sovereign_rise.app.data.remote.api.UsageStatsApiService {
        return com.sovereign_rise.app.data.remote.NetworkModule.createService()
    }
    
    /**
     * Provides UsageStatsRepository instance (singleton per app session).
     */
    fun provideUsageStatsRepository(context: Context): com.sovereign_rise.app.domain.repository.UsageStatsRepository {
        return usageStatsRepository ?: synchronized(this) {
            usageStatsRepository ?: com.sovereign_rise.app.data.repository.UsageStatsRepositoryImpl(
                context = context,
                usageStatsApi = provideUsageStatsApiService()
            ).also { usageStatsRepository = it }
        }
    }
    
    /**
     * Provides SmartReminderRepository instance (singleton per app session).
     */
    fun provideSmartReminderRepository(context: Context): com.sovereign_rise.app.domain.repository.SmartReminderRepository {
        return smartReminderRepository ?: synchronized(this) {
            smartReminderRepository ?: com.sovereign_rise.app.data.repository.SmartReminderRepositoryImpl(
                taskRepository = provideTaskRepository(context),
                completionPatternDao = provideUsageStatsDatabase(context).completionPatternDao()
            ).also { smartReminderRepository = it }
        }
    }
    
    /**
     * Provides BurnoutRepository instance (singleton per app session).
     */
    fun provideBurnoutRepository(context: Context): com.sovereign_rise.app.domain.repository.BurnoutRepository {
        return burnoutRepository ?: synchronized(this) {
            burnoutRepository ?: com.sovereign_rise.app.data.repository.BurnoutRepositoryImpl(
                taskRepository = provideTaskRepository(context),
                habitRepository = provideHabitRepository(context),
                usageStatsRepository = provideUsageStatsRepository(context),
                burnoutMetricsDao = provideUsageStatsDatabase(context).burnoutMetricsDao(),
                dataStore = provideTokenDataStore(context).dataStore
            ).also { burnoutRepository = it }
        }
    }
    
    /**
     * Provides AIApiService instance.
     */
    fun provideAIApiService(): AIApiService {
        return NetworkModule.createAIApiService()
    }
    
    /**
     * Provides AnalyticsApiService instance.
     */
    fun provideAnalyticsApiService(): com.sovereign_rise.app.data.remote.api.AnalyticsApiService {
        return NetworkModule.createAnalyticsApiService()
    }
    
    /**
     * Provides UserApiService instance.
     */
    fun provideUserApiService(): com.sovereign_rise.app.data.remote.api.UserApiService {
        return NetworkModule.createUserApiService()
    }
    
    /**
     * Provides AffirmationRepository instance (singleton per app session).
     */
    fun provideAffirmationRepository(context: Context): com.sovereign_rise.app.domain.repository.AffirmationRepository {
        return affirmationRepository ?: synchronized(this) {
            affirmationRepository ?: com.sovereign_rise.app.data.repository.AffirmationRepositoryImpl(
                context = context,
                dataStore = provideTokenDataStore(context).dataStore,
                aiApiService = provideAIApiService()
            ).also { affirmationRepository = it }
        }
    }
    
    /**
     * Provides CollectUsageStatsUseCase instance.
     */
    fun provideCollectUsageStatsUseCase(context: Context): com.sovereign_rise.app.domain.usecase.ai.CollectUsageStatsUseCase {
        return com.sovereign_rise.app.domain.usecase.ai.CollectUsageStatsUseCase(provideUsageStatsRepository(context))
    }
    
    /**
     * Provides GenerateSmartReminderUseCase instance.
     */
    fun provideGenerateSmartReminderUseCase(context: Context): com.sovereign_rise.app.domain.usecase.ai.GenerateSmartReminderUseCase {
        return com.sovereign_rise.app.domain.usecase.ai.GenerateSmartReminderUseCase(provideSmartReminderRepository(context))
    }
    
    /**
     * Provides DetectBurnoutUseCase instance.
     */
    fun provideDetectBurnoutUseCase(context: Context): com.sovereign_rise.app.domain.usecase.ai.DetectBurnoutUseCase {
        return com.sovereign_rise.app.domain.usecase.ai.DetectBurnoutUseCase(provideBurnoutRepository(context))
    }
    
    /**
     * Provides ActivateRecoveryModeUseCase instance.
     */
    fun provideActivateRecoveryModeUseCase(context: Context): com.sovereign_rise.app.domain.usecase.ai.ActivateRecoveryModeUseCase {
        return com.sovereign_rise.app.domain.usecase.ai.ActivateRecoveryModeUseCase(provideBurnoutRepository(context))
    }
    
    /**
     * Provides GenerateAffirmationUseCase instance.
     */
    fun provideGenerateAffirmationUseCase(context: Context): com.sovereign_rise.app.domain.usecase.ai.GenerateAffirmationUseCase {
        return com.sovereign_rise.app.domain.usecase.ai.GenerateAffirmationUseCase(provideAffirmationRepository(context))
    }
    
    /**
     * Provides CheckUsageNudgeTriggerUseCase instance.
     */
    fun provideCheckUsageNudgeTriggerUseCase(context: Context): com.sovereign_rise.app.domain.usecase.ai.CheckUsageNudgeTriggerUseCase {
        return com.sovereign_rise.app.domain.usecase.ai.CheckUsageNudgeTriggerUseCase(provideUsageStatsRepository(context))
    }
    
    /**
     * Provides AIFeaturesViewModel instance (singleton per app session).
     */
    fun provideAIFeaturesViewModel(context: Context): com.sovereign_rise.app.presentation.viewmodel.AIFeaturesViewModel {
        return aiFeaturesViewModel ?: synchronized(this) {
            aiFeaturesViewModel ?: com.sovereign_rise.app.presentation.viewmodel.AIFeaturesViewModel(
                usageStatsRepository = provideUsageStatsRepository(context),
                smartReminderRepository = provideSmartReminderRepository(context),
                burnoutRepository = provideBurnoutRepository(context),
                affirmationRepository = provideAffirmationRepository(context),
                collectUsageStatsUseCase = provideCollectUsageStatsUseCase(context),
                detectBurnoutUseCase = provideDetectBurnoutUseCase(context)
            ).also { aiFeaturesViewModel = it }
        }
    }
    
    // ==================== Offline Mode Providers ====================
    
    /**
     * Provides AppDatabase instance (singleton).
     */
    fun provideAppDatabase(context: Context): com.sovereign_rise.app.data.local.UsageStatsDatabase {
        return appDatabase ?: synchronized(this) {
            appDatabase ?: com.sovereign_rise.app.data.local.UsageStatsDatabase.getInstance(context)
                .also { appDatabase = it }
        }
    }
    
    /**
     * Provides TaskDao instance.
     */
    fun provideTaskDao(context: Context): com.sovereign_rise.app.data.local.dao.TaskDao {
        return provideAppDatabase(context).taskDao()
    }
    
    /**
     * Provides HabitDao instance.
     */
    fun provideHabitDao(context: Context): com.sovereign_rise.app.data.local.dao.HabitDao {
        return provideAppDatabase(context).habitDao()
    }
    
    /**
     * Provides UserDao instance.
     */
    fun provideUserDao(context: Context): com.sovereign_rise.app.data.local.dao.UserDao {
        return provideAppDatabase(context).userDao()
    }
    
    
    /**
     * Provides SyncQueueDao instance.
     */
    fun provideSyncQueueDao(context: Context): com.sovereign_rise.app.data.local.dao.SyncQueueDao {
        return provideAppDatabase(context).syncQueueDao()
    }
    
    /**
     * Provides ConnectivityObserver instance (singleton).
     */
    fun provideConnectivityObserver(context: Context): com.sovereign_rise.app.util.ConnectivityObserver {
        return connectivityObserver ?: synchronized(this) {
            connectivityObserver ?: com.sovereign_rise.app.util.ConnectivityObserver(context)
                .also { connectivityObserver = it }
        }
    }
    
    /**
     * Provides SyncManager instance (singleton).
     */
    fun provideSyncManager(context: Context): com.sovereign_rise.app.data.sync.SyncManager {
        return syncManager ?: synchronized(this) {
            syncManager ?: com.sovereign_rise.app.data.sync.SyncManager(
                taskDao = provideTaskDao(context),
                habitDao = provideHabitDao(context),
                userDao = provideUserDao(context),
                syncQueueDao = provideSyncQueueDao(context),
                taskApiService = provideTaskApiService(),
                habitApiService = provideHabitApiService(),
                context = context
            ).also { syncManager = it }
        }
    }
    
    /**
     * Provides UserRepository instance (singleton).
     */
    fun provideUserRepository(context: Context): com.sovereign_rise.app.domain.repository.UserRepository {
        return userRepository ?: synchronized(this) {
            userRepository ?: com.sovereign_rise.app.data.repository.UserRepositoryImpl(
                userDao = provideUserDao(context),
                authApiService = provideAuthApiService(),
                syncManager = provideSyncManager(context)
            ).also { userRepository = it }
        }
    }
    
    /**
     * Provides OnboardingRepository instance (singleton).
     */
    fun provideOnboardingRepository(context: Context): com.sovereign_rise.app.domain.repository.OnboardingRepository {
        return onboardingRepository ?: synchronized(this) {
            onboardingRepository ?: com.sovereign_rise.app.data.repository.OnboardingRepositoryImpl(
                dataStore = provideTokenDataStore(context).dataStore
            ).also { onboardingRepository = it }
        }
    }
    
    /**
     * Provides OnboardingViewModel instance (singleton per app session).
     */
    fun provideOnboardingViewModel(context: Context): com.sovereign_rise.app.presentation.viewmodel.OnboardingViewModel {
        return onboardingViewModel ?: synchronized(this) {
            onboardingViewModel ?: com.sovereign_rise.app.presentation.viewmodel.OnboardingViewModel(
                onboardingRepository = provideOnboardingRepository(context)
            ).also { onboardingViewModel = it }
        }
    }
    
    /**
     * Provides AnalyticsRepository instance (singleton).
     */
    fun provideAnalyticsRepository(context: Context): com.sovereign_rise.app.domain.repository.AnalyticsRepository {
        return analyticsRepository ?: synchronized(this) {
            analyticsRepository ?: com.sovereign_rise.app.data.repository.AnalyticsRepositoryImpl(
                analyticsApi = provideAnalyticsApiService(),
                taskDao = provideTaskDao(context),
                habitDao = provideHabitDao(context),
                database = provideUsageStatsDatabase(context),
                usageStatsRepository = provideUsageStatsRepository(context),
                connectivityObserver = provideConnectivityObserver(context)
            ).also { analyticsRepository = it }
        }
    }
    
    /**
     * Provides AnalyticsViewModel instance (singleton per app session).
     */
    fun provideAnalyticsViewModel(context: Context): com.sovereign_rise.app.presentation.viewmodel.AnalyticsViewModel {
        return analyticsViewModel ?: synchronized(this) {
            analyticsViewModel ?: com.sovereign_rise.app.presentation.viewmodel.AnalyticsViewModel(
                analyticsRepository = provideAnalyticsRepository(context),
                connectivityObserver = provideConnectivityObserver(context),
                tokenDataStore = provideTokenDataStore(context)
            ).also { analyticsViewModel = it }
        }
    }
    
    
    /**
     * Clears all cached instances (useful for logout or testing).
     */
    fun clear() {
        authViewModel = null
        profileViewModel = null
        taskViewModel = null
        habitViewModel = null
        homeViewModel = null
        authRepository = null
        taskRepository = null
        habitRepository = null
        aiFeaturesViewModel = null
        usageStatsRepository = null
        smartReminderRepository = null
        burnoutRepository = null
        affirmationRepository = null
        syncManager = null
        connectivityObserver = null
        userRepository = null
        onboardingRepository = null
        onboardingViewModel = null
        analyticsRepository = null
        analyticsViewModel = null
        // Don't clear appDatabase - it's persistent
    }
}
