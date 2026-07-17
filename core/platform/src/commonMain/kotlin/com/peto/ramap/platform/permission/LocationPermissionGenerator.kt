package com.peto.ramap.platform.permission

interface LocationPermissionGenerator {
    fun hasPermission(): Boolean

    fun requestPermission()
}
