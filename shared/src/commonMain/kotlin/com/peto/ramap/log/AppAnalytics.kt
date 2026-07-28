package com.peto.ramap.log

import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.AnalyticsUserProperties
import com.peto.ramap.analytics.common.deeplink.DeepLinkAnalyticsEvent
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.personalization.ShopPersonalization

class AppAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logScreenView(screenName: String) {
        analyticsTracker.logScreenView(screenName)
    }

    fun logNotificationOpened(eventId: String) {
        analyticsTracker.logEvent(
            NotificationOpened(
                eventId = eventId,
            ),
        )
    }

    fun logSharedShopLinkOpened(shopId: String) {
        analyticsTracker.logEvent(SharedShopLinkOpened(shopId))
    }

    fun logDeepLinkReceived() = analyticsTracker.logEvent(DeepLinkAnalyticsEvent.Received)

    fun logDeepLinkParseSucceeded(shopId: String) = analyticsTracker.logEvent(DeepLinkAnalyticsEvent.ParseSucceeded(shopId))

    fun logDeepLinkParseFailed() = analyticsTracker.logEvent(DeepLinkAnalyticsEvent.ParseFailed)

    fun logDeepLinkNavigationSucceeded(shopId: String) = analyticsTracker.logEvent(DeepLinkAnalyticsEvent.NavigationSucceeded(shopId))

    fun logDeepLinkNavigationFailed(shopId: String? = null) = analyticsTracker.logEvent(DeepLinkAnalyticsEvent.NavigationFailed(shopId))

    fun updateLoginStatus(state: LoginSessionState) {
        analyticsTracker.userProperty(
            AnalyticsUserProperties.LOGIN_STATUS,
            loginStatus(state),
        )
    }

    fun updatePersonalizationProperties(personalization: ShopPersonalization) {
        analyticsTracker.userProperty(
            AnalyticsUserProperties.BOOKMARKED_COUNT,
            personalization.bookmarkedShopIds.size.toString(),
        )

        analyticsTracker.userProperty(
            AnalyticsUserProperties.SUBSCRIBED_COUNT,
            personalization.notificationShopIds.size.toString(),
        )

        analyticsTracker.userProperty(
            AnalyticsUserProperties.HIDDEN_COUNT,
            personalization.hiddenShopIds.size.toString(),
        )
    }

    private fun loginStatus(state: LoginSessionState): String =
        if (state == LoginSessionState.AUTHENTICATED) {
            LOGIN_STATUS_LOGGED_IN
        } else {
            LOGIN_STATUS_GUEST
        }

    companion object {
        private const val LOGIN_STATUS_LOGGED_IN = "logged_in"
        private const val LOGIN_STATUS_GUEST = "guest"
    }
}
