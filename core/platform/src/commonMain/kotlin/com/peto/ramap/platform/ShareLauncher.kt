package com.peto.ramap.platform

expect object ShareLauncher {
    fun share(
        text: String,
        chooserTitle: String? = null,
    )
}
