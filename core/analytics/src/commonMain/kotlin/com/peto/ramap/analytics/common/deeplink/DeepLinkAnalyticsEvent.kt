package com.peto.ramap.analytics.common.deeplink

import com.peto.ramap.analytics.AnalyticsEvent

sealed interface DeepLinkAnalyticsEvent : AnalyticsEvent {
    val shopId: String?
        get() = null
    val failureReason: String?
        get() = null

    override fun params(): Map<String, Any> =
        buildMap {
            put(LINK_TYPE_PARAMETER, LINK_TYPE_SHOP)
            shopId?.let { put("shop_id", it) }
            failureReason?.let { put("failure_reason", it) }
        }

    data object Received : DeepLinkAnalyticsEvent {
        override val name: String = "deep_link_received"
    }

    data class ParseSucceeded(
        override val shopId: String,
    ) : DeepLinkAnalyticsEvent {
        override val name: String = "deep_link_parse_succeeded"
    }

    data object ParseFailed : DeepLinkAnalyticsEvent {
        override val name: String = "deep_link_parse_failed"
        override val failureReason: String = INVALID_URL
    }

    data class NavigationSucceeded(
        override val shopId: String,
    ) : DeepLinkAnalyticsEvent {
        override val name: String = "deep_link_navigation_succeeded"
    }

    data class NavigationFailed(
        override val shopId: String? = null,
    ) : DeepLinkAnalyticsEvent {
        override val name: String = "deep_link_navigation_failed"
        override val failureReason: String = NAVIGATION_ERROR
    }

    companion object {
        const val LINK_TYPE_SHOP = "shop"
        private const val LINK_TYPE_PARAMETER = "link_type"
        private const val INVALID_URL = "invalid_url"
        private const val NAVIGATION_ERROR = "navigation_error"
    }
}
