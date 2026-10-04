package com.peto.ramap.domain.usecase

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.analytics.common.login.LoginMethod
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.repository.LoginRepository

class SignInUseCase(
    private val loginRepository: LoginRepository,
    private val loginAnalytics: LoginAnalytics,
) {
    suspend operator fun invoke(
        type: LoginType,
        source: AnalyticsSource,
    ): RamapResult<Unit> {
        val method =
            when (type) {
                LoginType.KAKAO -> LoginMethod.KAKAO
                LoginType.APPLE -> LoginMethod.APPLE
            }
        loginAnalytics.logLoginStarted(source, method)

        val result = loginRepository.signIn(type)
        when (result) {
            is RamapResult.Success -> loginAnalytics.logLoginSucceeded(source, method)
            is RamapResult.Error -> loginAnalytics.logLoginFailed(source, method)
        }
        return result
    }
}
