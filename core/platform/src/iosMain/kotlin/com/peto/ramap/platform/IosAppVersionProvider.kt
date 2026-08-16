package com.peto.ramap.platform

import platform.Foundation.NSBundle

class IosAppVersionProvider : AppVersionProvider {
    override val versionName: String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
    override val buildNumber: Long =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)?.toLongOrNull() ?: 0L
    override val platform: String = PLATFORM

    private companion object {
        const val PLATFORM = "ios"
    }
}
