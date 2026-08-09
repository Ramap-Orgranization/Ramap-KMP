package com.peto.ramap.platform.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidNetworkConnectivityObserver(
    context: Context,
) : NetworkConnectivityObserver {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observe(): Flow<NetworkConnectivityStatus> =
        callbackFlow {
            trySend(currentStatus())
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        trySend(networkConnectivityStatus(networkCapabilities))
                    }

                    override fun onLost(network: Network) {
                        trySend(NetworkConnectivityStatus.Unavailable)
                    }
                }
            connectivityManager.registerDefaultNetworkCallback(callback)
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }.distinctUntilChanged()

    private fun currentStatus(): NetworkConnectivityStatus =
        connectivityManager.activeNetwork
            ?.let(connectivityManager::getNetworkCapabilities)
            ?.let(::networkConnectivityStatus)
            ?: NetworkConnectivityStatus.Unavailable

    private companion object {
        fun networkConnectivityStatus(capabilities: NetworkCapabilities): NetworkConnectivityStatus =
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {
                NetworkConnectivityStatus.Available
            } else {
                NetworkConnectivityStatus.Unavailable
            }
    }
}
