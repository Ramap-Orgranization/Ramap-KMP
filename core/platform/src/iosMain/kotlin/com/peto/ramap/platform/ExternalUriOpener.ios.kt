package com.peto.ramap.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object ExternalUriOpener {
    actual val isAppleMapsAvailable: Boolean = true

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

    actual fun startAppUpdate(uri: String) = open(uri)

    actual fun resumeAppUpdate() = Unit

    actual fun isSupportedWebUri(uri: String): Boolean {
        val normalizedUri = uri.trim().lowercase()
        return normalizedUri.startsWith("https://") || normalizedUri.startsWith("http://")
    }

    actual fun openAppleMaps(
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
    ) {
        val query = listOf(name, address).joinToString(", ")
        val encodedQuery = encodeUrlComponent(query)
        val uri = "https://maps.apple.com/?q=$encodedQuery&ll=$latitude,$longitude"
        open(uri)
    }

    private fun encodeUrlComponent(value: String): String =
        buildString {
            value.encodeToByteArray().forEach { byte ->
                val character = byte.toInt() and 0xFF
                if (character in 0x30..0x39 ||
                    character in 0x41..0x5A ||
                    character in 0x61..0x7A ||
                    character == 0x2D ||
                    character == 0x2E ||
                    character == 0x5F ||
                    character == 0x7E
                ) {
                    append(character.toChar())
                } else {
                    append('%')
                    append(HEX_DIGITS[character shr 4])
                    append(HEX_DIGITS[character and 0x0F])
                }
            }
        }

    private fun isSupportedUri(uri: String): Boolean = isSupportedWebUri(uri) || uri.lowercase().startsWith("tel:")

    private const val HEX_DIGITS = "0123456789ABCDEF"
}
