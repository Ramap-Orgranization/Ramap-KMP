package com.peto.ramap.ui.account

import com.peto.ramap.analytics.AnalyticsEvents
import com.peto.ramap.analytics.AnalyticsParams
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker

class AccountAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logLoginStarted() {
        analyticsTracker.logEvent(
            AnalyticsEvents.LOGIN_START,
            mapOf(
                AnalyticsParams.SOURCE to AnalyticsSource.ACCOUNT,
            ),
        )
    }

    fun logLoginSucceeded() {
        analyticsTracker.logEvent(
            AnalyticsEvents.LOGIN_SUCCESS,
            kakaoLoginParameters(),
        )
    }

    fun logLoginFailed() {
        analyticsTracker.logEvent(
            AnalyticsEvents.LOGIN_FAILURE,
            kakaoLoginParameters(),
        )
    }

    private fun kakaoLoginParameters(): Map<String, String> =
        mapOf(
            AnalyticsParams.METHOD to KAKAO_LOGIN_METHOD,
            AnalyticsParams.SOURCE to AnalyticsSource.ACCOUNT,
        )

    companion object {
        private const val KAKAO_LOGIN_METHOD = "kakao"
    }
}
