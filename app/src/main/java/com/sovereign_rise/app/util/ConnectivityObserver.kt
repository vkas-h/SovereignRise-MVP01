package com.sovereign_rise.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Enum representing connectivity status.
 */
enum class ConnectivityStatus {
    AVAILABLE,
    UNAVAILABLE,
    LOSING,
    UNKNOWN
}

/**
 * Enum representing network type.
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    NONE
}

/**
 * Utility class for observing network connectivity changes.
 */
class ConnectivityObserver(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Observes network connectivity changes as a Flow.
     */
    fun observe(): Flow<ConnectivityStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(ConnectivityStatus.AVAILABLE)
            }
            
            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(ConnectivityStatus.UNAVAILABLE)
            }
            
            override fun onUnavailable() {
                super.onUnavailable()
                trySend(ConnectivityStatus.UNAVAILABLE)
            }
            
            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                trySend(ConnectivityStatus.LOSING)
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, callback)
        
        // Send initial state
        trySend(if (isOnline()) ConnectivityStatus.AVAILABLE else ConnectivityStatus.UNAVAILABLE)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
    
    /**
     * Checks current connectivity state synchronously.
     * Returns true only if network is connected AND validated (has actual internet access).
     */
    fun isOnline(): Boolean {
        try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            // Check for both internet capability AND validation
            // NET_CAPABILITY_VALIDATED ensures we have actual internet, not just WiFi connection
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            // If any error, assume offline for safety
            return false
        }
    }
    
    /**
     * Returns the current network type.
     */
    fun getNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            else -> NetworkType.NONE
        }
    }
}

/**
 * Extension function to observe connectivity as a Compose State.
 * Makes it easy to use ConnectivityObserver in Composables.
 */
@Composable
fun ConnectivityObserver.observeAsState(): State<ConnectivityStatus> {
    return observe().collectAsState(initial = ConnectivityStatus.UNAVAILABLE)
}

