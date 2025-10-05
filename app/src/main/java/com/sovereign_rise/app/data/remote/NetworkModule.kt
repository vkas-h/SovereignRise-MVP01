package com.sovereign_rise.app.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sovereign_rise.app.data.remote.api.AuthApiService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.sovereign_rise.app.BuildConfig

object NetworkModule {
    
    // Using local IP address to bypass firewall/connection issues
    // Your computer's IP: 172.20.154.3
    // For Production: Use "https://api.sovereignrise.com/"
    private const val BASE_URL = "http://172.20.154.3:3000/"
    
    // Reduced timeouts for better offline experience
    private const val CONNECT_TIMEOUT = 5L
    private const val READ_TIMEOUT = 10L
    private const val WRITE_TIMEOUT = 10L
    
    // Cached token for auth interceptor - will be set by AppModule
    // TODO: Enhance with token refresh logic in future phases
    @Volatile
    private var cachedToken: String? = null
    
    /**
     * Sets the cached authentication token for API requests.
     */
    fun setAuthToken(token: String?) {
        cachedToken = token
    }
    
    private val gson: Gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }
    
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    /**
     * Auth interceptor that automatically attaches Firebase tokens to API requests.
     */
    private val authInterceptor: Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // Skip if the request already has an Authorization header
        if (originalRequest.header("Authorization") != null) {
            Log.d("NetworkModule", "Request already has auth header")
            return@Interceptor chain.proceed(originalRequest)
        }
        
        // Add token to request if available
        val token = cachedToken
        val newRequest = if (token != null) {
            Log.d("NetworkModule", "Adding auth token to request: ${originalRequest.url}")
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            Log.w("NetworkModule", "No cached token available for request: ${originalRequest.url}")
            originalRequest
        }
        
        chain.proceed(newRequest)
    }
    
    /**
     * Token refresh authenticator - automatically refreshes expired Firebase tokens.
     * This is called by OkHttp when a request returns 401 Unauthorized.
     */
    private val tokenAuthenticator = Authenticator { route: Route?, response: Response ->
        // Only refresh token if we got a 401 and we have a Firebase user
        if (response.code == 401) {
            val firebaseAuth = FirebaseAuth.getInstance()
            val currentUser = firebaseAuth.currentUser
            
            if (currentUser != null) {
                try {
                    Log.d("NetworkModule", "Token expired, refreshing...")
                    
                    // Refresh token synchronously (we're already on a background thread)
                    val newToken = runBlocking {
                        currentUser.getIdToken(true).await().token
                    }
                    
                    if (newToken != null) {
                        Log.d("NetworkModule", "Token refreshed successfully")
                        
                        // Update cached token
                        cachedToken = newToken
                        
                        // Retry the request with new token
                        return@Authenticator response.request.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                    } else {
                        Log.e("NetworkModule", "Failed to refresh token - token is null")
                    }
                } catch (e: Exception) {
                    Log.e("NetworkModule", "Token refresh failed: ${e.message}", e)
                }
            } else {
                Log.e("NetworkModule", "Token expired but no Firebase user found")
            }
        }
        
        // Return null to give up on retrying (user will need to re-login)
        null
    }
    
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator) // Automatically refreshes expired tokens
            .build()
    }
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Creates a Retrofit service for the given API interface.
     * Usage: val authService = NetworkModule.createService<AuthApiService>()
     */
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
    
    // Inline reified version for easier Kotlin usage
    inline fun <reified T> createService(): T = createService(T::class.java)
    
    /**
     * Convenience function to create an AuthApiService instance.
     */
    fun createAuthApiService(): AuthApiService = createService()
    
    /**
     * Convenience function to create a TaskApiService instance.
     */
    fun createTaskApiService(): com.sovereign_rise.app.data.remote.api.TaskApiService = createService()
    
    /**
     * Convenience function to create a HabitApiService instance.
     */
    fun createHabitApiService(): com.sovereign_rise.app.data.remote.api.HabitApiService = createService()
    
    /**
     * Convenience function to create an AIApiService instance.
     */
    fun createAIApiService(): com.sovereign_rise.app.data.remote.api.AIApiService = createService()
    
    /**
     * Convenience function to create an AnalyticsApiService instance.
     */
    fun createAnalyticsApiService(): com.sovereign_rise.app.data.remote.api.AnalyticsApiService = createService()
    
    /**
     * Convenience function to create a UserApiService instance.
     */
    fun createUserApiService(): com.sovereign_rise.app.data.remote.api.UserApiService = createService()
}
