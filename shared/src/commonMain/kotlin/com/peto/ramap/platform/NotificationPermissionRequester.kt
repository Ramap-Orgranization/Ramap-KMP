package com.peto.ramap.platform

expect object NotificationPermissionRequester {
    suspend fun request(): Boolean
}
