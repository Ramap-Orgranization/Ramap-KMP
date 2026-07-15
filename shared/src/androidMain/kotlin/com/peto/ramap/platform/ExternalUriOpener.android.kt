package com.peto.ramap.platform

import android.app.Activity
import android.content.Intent
import android.net.Uri

actual object ExternalUriOpener {
    private var activity: Activity? = null

    fun attach(activity: Activity) {
        this.activity = activity
    }

    fun detach(activity: Activity) {
        if (this.activity === activity) this.activity = null
    }

    actual fun open(uri: String) {
        val normalizedUri = uri.trim()
        if (!isSupportedUri(normalizedUri)) return

        runCatching {
            activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUri)))
        }
    }

    actual fun isSupportedWebUri(uri: String): Boolean {
        val normalizedUri = uri.trim().lowercase()
        return normalizedUri.startsWith("https://") || normalizedUri.startsWith("http://")
    }

    private fun isSupportedUri(uri: String): Boolean = isSupportedWebUri(uri) || uri.lowercase().startsWith("tel:")
}
