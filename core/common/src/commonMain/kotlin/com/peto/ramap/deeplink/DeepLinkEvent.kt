package com.peto.ramap.deeplink

sealed interface DeepLinkEvent {
    data class Url(
        val value: String,
    ) : DeepLinkEvent

    data class Notification(
        val value: String,
    ) : DeepLinkEvent
}
