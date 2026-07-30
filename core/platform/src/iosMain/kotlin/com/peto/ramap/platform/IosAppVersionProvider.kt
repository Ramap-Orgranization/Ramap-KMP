package com.peto.ramap.platform

import platform.Foundation.NSBundle

class IosAppVersionProvider : AppVersionProvider {
    override val versionName: String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
}
