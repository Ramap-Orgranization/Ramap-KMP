package com.peto.ramap.platform

interface AppVersionProvider {
    val versionName: String

    val buildNumber: Long

    val platform: String
}
