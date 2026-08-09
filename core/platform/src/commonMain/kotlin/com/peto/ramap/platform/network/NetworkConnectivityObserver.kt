package com.peto.ramap.platform.network

import kotlinx.coroutines.flow.Flow

interface NetworkConnectivityObserver {
    fun observe(): Flow<NetworkConnectivityStatus>
}
