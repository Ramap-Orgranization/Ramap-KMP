package com.peto.ramap.domain.usecase

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignInUseCaseTest {
    @Test
    fun `로그인 성공 시 애널리틱스 성공 이벤트를 기록하고 Success 결과를 반환한다`() =
        runTest {
            val repository = FakeLoginRepository()
            val tracker = FakeAnalyticsTracker()
            val analytics = LoginAnalytics(tracker, FakeCrashReporter())
            val useCase = SignInUseCase(repository, analytics)

            val result = useCase(LoginType.KAKAO, AnalyticsSource.ACCOUNT)

            assertTrue(result is RamapResult.Success)
            assertEquals(1, repository.signInWithKakaoCallCount)
            assertEquals(2, tracker.events.size)
        }

    @Test
    fun `로그인 실패 시 애널리틱스 실패 이벤트를 기록하고 Error 결과를 반환한다`() =
        runTest {
            val repository =
                FakeLoginRepository().apply {
                    signInResult = RamapError.Unknown(IllegalStateException("Login failed")).let { RamapResult.Error(it) }
                }
            val tracker = FakeAnalyticsTracker()
            val analytics = LoginAnalytics(tracker, FakeCrashReporter())
            val useCase = SignInUseCase(repository, analytics)

            val result = useCase(LoginType.APPLE, AnalyticsSource.MAP)

            assertTrue(result is RamapResult.Error)
            assertEquals(1, repository.signInWithAppleCallCount)
            assertEquals(2, tracker.events.size)
        }
}
