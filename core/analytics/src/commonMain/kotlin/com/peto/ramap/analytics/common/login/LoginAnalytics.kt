package com.peto.ramap.analytics.common.login

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker

class LoginAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logLoginStarted(source: AnalyticsSource) {
        analyticsTracker.logEvent(
            LoginEvent.LoginStarted(
                method = LoginMethod.KAKAO,
                source = source,
            ),
        )
    }

    fun logLoginSucceeded(source: AnalyticsSource) {
        analyticsTracker.logEvent(
            LoginEvent.LoginSucceeded(
                method = LoginMethod.KAKAO,
                source = source,
            ),
        )
    }

    fun logLoginFailed(source: AnalyticsSource) {
        analyticsTracker.logEvent(
            LoginEvent.LoginFailed(
                method = LoginMethod.KAKAO,
                source = source,
            ),
        )
    }
}
