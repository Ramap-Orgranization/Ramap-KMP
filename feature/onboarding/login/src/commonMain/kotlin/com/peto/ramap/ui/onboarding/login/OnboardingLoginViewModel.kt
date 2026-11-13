package com.peto.ramap.ui.onboarding.login

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.onboarding.login.contract.OnboardingLoginIntent
import com.peto.ramap.ui.onboarding.login.contract.OnboardingLoginLoadKey
import com.peto.ramap.ui.onboarding.login.contract.OnboardingLoginSideEffect
import com.peto.ramap.ui.onboarding.login.contract.OnboardingLoginUiState
import com.peto.ramap.ui.task.TaskPolicy
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.apple_login_failure_message
import ramap.shared.generated.resources.kakao_login_failure_message

internal class OnboardingLoginViewModel(
    private val loginRepository: LoginRepository,
    private val signInUseCase: SignInUseCase,
) : BaseViewModel<OnboardingLoginUiState, OnboardingLoginIntent, OnboardingLoginSideEffect>(OnboardingLoginUiState()) {
    override suspend fun handleIntent(intent: OnboardingLoginIntent) {
        when (intent) {
            is OnboardingLoginIntent.SelectLoginType -> signIn(intent.type)
            OnboardingLoginIntent.ContinueWithoutLogin -> continueWithoutLogin()
        }
    }

    private suspend fun signIn(type: LoginType) {
        if (currentState.isSigningIn) return
        if (loginRepository.hasSession()) {
            postSideEffect(OnboardingLoginSideEffect.Completed)
            return
        }
        launchResultTask(
            taskKey = SIGN_IN_TASK_KEY,
            loadKey = OnboardingLoginLoadKey.SignIn,
            policy = TaskPolicy.IgnoreNew,
            request = { signInUseCase(type, AnalyticsSource.ONBOARDING) },
            onSuccess = { postSideEffect(OnboardingLoginSideEffect.Completed) },
            onError = {
                val message =
                    when (type) {
                        LoginType.KAKAO -> Res.string.kakao_login_failure_message
                        LoginType.APPLE -> Res.string.apple_login_failure_message
                    }
                postSideEffect(OnboardingLoginSideEffect.ShowToast(ToastData(message, ToastType.ERROR)))
            },
        )
    }

    private suspend fun continueWithoutLogin() {
        cancelTask(SIGN_IN_TASK_KEY)
        postSideEffect(OnboardingLoginSideEffect.Completed)
    }

    private companion object {
        const val SIGN_IN_TASK_KEY = "onboarding-sign-in"
    }
}
