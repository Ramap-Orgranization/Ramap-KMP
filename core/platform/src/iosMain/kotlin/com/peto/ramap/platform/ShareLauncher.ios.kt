package com.peto.ramap.platform

import platform.Foundation.NSNotificationCenter

actual object ShareLauncher {
    actual fun share(
        text: String,
        chooserTitle: String?,
    ) {
        if (text.isBlank()) return
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = SHARE_REQUEST_NOTIFICATION,
            `object` = text,
        )
    }

    private const val SHARE_REQUEST_NOTIFICATION = "ShopShareRequest"
}
