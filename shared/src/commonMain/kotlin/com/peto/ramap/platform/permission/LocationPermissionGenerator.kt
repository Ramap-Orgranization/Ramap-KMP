package com.peto.ramap.platform.permission

internal interface LocationPermissionGenerator {
    fun hasPermission(): Boolean

    fun requestPermission()
}
