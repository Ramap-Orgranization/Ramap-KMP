package com.peto.ramap.platform

expect object NotificationPermissionRequester {
    val isSupported: Boolean

    suspend fun isGranted(): Boolean

    suspend fun request(): Boolean
}
