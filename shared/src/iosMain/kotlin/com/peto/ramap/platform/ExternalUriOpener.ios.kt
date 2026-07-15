package com.peto.ramap.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object ExternalUriOpener {
    actual fun open(uri: String) {
        val normalizedUri = uri.trim()
        if (!isSupportedUri(normalizedUri)) return
        val url = NSURL.URLWithString(normalizedUri) ?: return

        UIApplication.sharedApplication.openURL(
            url = url,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
    }

    actual fun isSupportedWebUri(uri: String): Boolean {
        val normalizedUri = uri.trim().lowercase()
        return normalizedUri.startsWith("https://") || normalizedUri.startsWith("http://")
    }

    private fun isSupportedUri(uri: String): Boolean = isSupportedWebUri(uri) || uri.lowercase().startsWith("tel:")
}
