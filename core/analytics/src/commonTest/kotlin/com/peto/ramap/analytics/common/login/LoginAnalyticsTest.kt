package com.peto.ramap.analytics.common.login

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LoginAnalyticsTest {
    @Test
    fun `로그인 실패를 분석 이벤트와 비민감 Crashlytics 메타데이터로 기록한다`() {
        val analyticsTracker = FakeAnalyticsTracker()
        val crashReporter = FakeCrashReporter()
        val loginAnalytics = LoginAnalytics(analyticsTracker, crashReporter)

        loginAnalytics.logLoginFailed(
            source = AnalyticsSource.MAP,
            method = LoginMethod.APPLE,
        )

        val event = assertIs<LoginEvent.LoginFailed>(analyticsTracker.events.single())
        assertEquals(LoginMethod.APPLE, event.method)
        assertEquals(AnalyticsSource.MAP, event.source)
        assertEquals(
            mapOf(
                "login_method" to "apple",
                "login_source" to "map",
            ),
            crashReporter.customKeys,
        )
        assertIs<LoginFailureException>(crashReporter.exceptions.single())
    }
}
