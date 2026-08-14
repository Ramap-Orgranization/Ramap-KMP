package com.peto.ramap.platform

actual object NotificationPermissionRequester {
    actual val isSupported = false

    actual suspend fun isGranted() = false

    actual suspend fun request() = false
}
