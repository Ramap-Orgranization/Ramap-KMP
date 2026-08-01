package com.peto.ramap.analytics.common.login

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.CrashReporter

class LoginAnalytics(
    private val analyticsTracker: AnalyticsTracker,
    private val crashReporter: CrashReporter,
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
        crashReporter.setCustomKey(LOGIN_METHOD_KEY, method.value)
        crashReporter.setCustomKey(LOGIN_SOURCE_KEY, source.value)
        crashReporter.recordException(LoginFailureException())
    }

    private companion object {
        const val LOGIN_METHOD_KEY = "login_method"
        const val LOGIN_SOURCE_KEY = "login_source"
    }
}
