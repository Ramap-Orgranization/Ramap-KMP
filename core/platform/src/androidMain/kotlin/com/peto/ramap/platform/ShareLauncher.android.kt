package com.peto.ramap.platform

import android.app.Activity
import android.content.Intent

actual object ShareLauncher {
    private var activity: Activity? = null

    fun attach(activity: Activity) {
        this.activity = activity
    }

    fun detach(activity: Activity) {
        if (this.activity === activity) this.activity = null
    }

    actual fun share(
        text: String,
        chooserTitle: String?,
    ) {
        if (text.isBlank()) return
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        activity?.startActivity(Intent.createChooser(sendIntent, chooserTitle))
    }
}
