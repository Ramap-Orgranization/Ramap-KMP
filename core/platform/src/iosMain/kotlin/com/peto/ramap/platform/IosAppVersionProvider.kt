package com.peto.ramap.platform

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
class IosAppVersionProvider : AppVersionProvider {
    override val versionName: String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
    override val buildNumber: Long =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)?.toLongOrNull() ?: 0L
    override val platform: String = PLATFORM
    override val isDebugBuild: Boolean = Platform.isDebugBinary

    private companion object {
        const val PLATFORM = "ios"
    }
}
