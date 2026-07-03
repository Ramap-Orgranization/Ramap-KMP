package com.peto.ramap.ui.login

import com.peto.ramap.core.base.BaseViewModel
import com.peto.ramap.domain.model.LoginType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.ui.login.contract.LoginIntent
import com.peto.ramap.ui.login.contract.LoginSideEffect
import com.peto.ramap.ui.login.contract.LoginUiState

class LoginViewModel(
    private val loginRepository: LoginRepository,
) : BaseViewModel<LoginUiState, LoginIntent, LoginSideEffect>(initialState = LoginUiState()) {
    override suspend fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.ClickLogin -> signIn(intent.type)
        }
    }

    private suspend fun signIn(type: LoginType) {
        when (type) {
            LoginType.KAKAO -> loginRepository.signInWithKakao()
        }
        if (loginRepository.hasSession()) {
            postSideEffect(LoginSideEffect.LoginSuccess)
        }
    }
}
