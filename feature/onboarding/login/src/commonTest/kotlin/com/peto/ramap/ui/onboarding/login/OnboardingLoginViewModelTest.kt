package com.peto.ramap.ui.onboarding.login

import app.cash.turbine.test
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.auth.SignupConsentDismissedException
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.ui.onboarding.login.contract.OnboardingLoginIntent
import com.peto.ramap.ui.onboarding.login.contract.OnboardingLoginSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.kakao_login_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingLoginViewModelTest {
    @Test
    fun `로그인 실패 후에는 선택을 다시 할 수 있고 성공하면 다음 단계로 이어진다`() =
        coroutinesTest {
            val login =
                FakeLoginRepository().apply {
                    signInResult = RamapResult.Error(RamapError.Unknown(IllegalStateException()))
                }
            val viewModel = createViewModel(login)

            viewModel.sideEffect.test {
                viewModel.dispatch(OnboardingLoginIntent.SelectLoginType(LoginType.KAKAO))
                assertEquals(
                    OnboardingLoginSideEffect.ShowToast(ToastData(Res.string.kakao_login_failure_message, ToastType.ERROR)),
                    awaitItem(),
                )
                assertFalse(viewModel.uiState.value.isSigningIn)

                login.signInResult = RamapResult.Success(Unit)
                viewModel.dispatch(OnboardingLoginIntent.SelectLoginType(LoginType.KAKAO))
                assertEquals(OnboardingLoginSideEffect.Completed, awaitItem())
            }
        }

    @Test
    fun `가입 약관을 닫아도 오류를 표시하지 않고 로그인 없이 시작할 수 있다`() =
        coroutinesTest {
            val login =
                object : LoginRepository by FakeLoginRepository() {
                    override suspend fun signIn(type: LoginType): RamapResult<Unit> = throw SignupConsentDismissedException()
                }
            val viewModel = createViewModel(login)

            viewModel.sideEffect.test {
                viewModel.dispatch(OnboardingLoginIntent.SelectLoginType(LoginType.KAKAO))
                runCurrent()
                expectNoEvents()
                assertFalse(viewModel.uiState.value.isSigningIn)

                viewModel.dispatch(OnboardingLoginIntent.ContinueWithoutLogin)
                assertEquals(OnboardingLoginSideEffect.Completed, awaitItem())
            }
        }

    @Test
    fun `로그인 응답을 기다리는 중에 건너뛰기 입력을 처리하면 로그인 작업을 취소한다`() =
        coroutinesTest {
            val cancelled = CompletableDeferred<Unit>()
            val login =
                object : LoginRepository by FakeLoginRepository() {
                    override suspend fun signIn(type: LoginType): RamapResult<Unit> =
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled.complete(Unit)
                        }
                }
            val viewModel = createViewModel(login)

            viewModel.sideEffect.test {
                viewModel.dispatch(OnboardingLoginIntent.SelectLoginType(LoginType.KAKAO))
                runCurrent()
                assertTrue(viewModel.uiState.value.isSigningIn)

                viewModel.dispatch(OnboardingLoginIntent.ContinueWithoutLogin)
                assertEquals(OnboardingLoginSideEffect.Completed, awaitItem())
                assertFalse(viewModel.uiState.value.isSigningIn)
                assertTrue(cancelled.isCompleted)
            }
        }

    private fun createViewModel(login: LoginRepository): OnboardingLoginViewModel =
        OnboardingLoginViewModel(
            loginRepository = login,
            signInUseCase = SignInUseCase(login, LoginAnalytics(FakeAnalyticsTracker(), FakeCrashReporter())),
        )
}
