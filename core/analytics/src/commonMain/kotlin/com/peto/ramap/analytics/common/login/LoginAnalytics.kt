package com.peto.ramap.analytics.common.login

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker

class LoginAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logLoginStarted(
        source: AnalyticsSource,
        method: LoginMethod = LoginMethod.KAKAO,
    ) {
        analyticsTracker.logEvent(
            LoginEvent.LoginStarted(
                method = method,
                source = source,
            ),
        )
    }

    fun logLoginSucceeded(
        source: AnalyticsSource,
        method: LoginMethod = LoginMethod.KAKAO,
    ) {
        analyticsTracker.logEvent(
            LoginEvent.LoginSucceeded(
                method = method,
                source = source,
            ),
        )
    }

    fun logLoginFailed(
        source: AnalyticsSource,
        method: LoginMethod = LoginMethod.KAKAO,
    ) {
        analyticsTracker.logEvent(
            LoginEvent.LoginFailed(
                method = method,
                source = source,
            ),
        )
    }
}
