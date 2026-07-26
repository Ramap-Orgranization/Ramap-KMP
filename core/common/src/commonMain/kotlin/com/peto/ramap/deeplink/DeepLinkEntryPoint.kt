package com.peto.ramap.deeplink

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeepLinkEntryPoint {
    private val _events = MutableStateFlow<DeepLinkEvent?>(null)
    val events = _events.asStateFlow()

    fun submitUrl(value: String?) {
        value?.trim()?.takeIf(String::isNotEmpty)?.let { _events.value = DeepLinkEvent.Url(it) }
    }

    fun submitNotification(value: String?) {
        value?.trim()?.takeIf(String::isNotEmpty)?.let { _events.value = DeepLinkEvent.Notification(it) }
    }

    fun consume(event: DeepLinkEvent) {
        _events.compareAndSet(event, null)
    }
}
