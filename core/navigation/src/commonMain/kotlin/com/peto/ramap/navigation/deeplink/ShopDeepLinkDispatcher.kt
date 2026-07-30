package com.peto.ramap.navigation.deeplink

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShopDeepLinkDispatcher(
    private val parser: ShopDeepLinkParser,
) {
    private val _pendingDeepLink = MutableStateFlow<String?>(null)
    val pendingDeepLink = _pendingDeepLink.asStateFlow()

    fun dispatch(rawUrl: String?): Boolean {
        if (parser.parse(rawUrl) == null) return false
        _pendingDeepLink.value = rawUrl
        return true
    }

    fun consume(rawUrl: String) {
        _pendingDeepLink.compareAndSet(rawUrl, null)
    }
}
