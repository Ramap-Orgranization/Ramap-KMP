package com.peto.ramap.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

class IosAppSettingsOpener : AppSettingsOpener {
    override fun open() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return

        UIApplication.sharedApplication.openURL(
            url = settingsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
    }
}
