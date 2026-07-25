package com.peto.ramap.analytics.common.login

import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsSource

sealed interface LoginEvent : AnalyticsEvent {
    val method: LoginMethod
    val source: AnalyticsSource

    override fun params(): Map<String, Any> =
        mapOf(
            "method" to method.value,
            "source" to source.value,
        )

    data class LoginStarted(
        override val method: LoginMethod,
        override val source: AnalyticsSource,
    ) : LoginEvent {
        override val name: String = "login_start"
    }

    data class LoginSucceeded(
        override val method: LoginMethod,
        override val source: AnalyticsSource,
    ) : LoginEvent {
        override val name: String = "login_success"
    }

    data class LoginFailed(
        override val method: LoginMethod,
        override val source: AnalyticsSource,
    ) : LoginEvent {
        override val name: String = "login_failure"
    }
}
