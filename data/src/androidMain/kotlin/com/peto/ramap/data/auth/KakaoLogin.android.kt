package com.peto.ramap.data.auth

import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine

internal actual suspend fun loginWithKakao(): KakaoToken {
    val token = loginWithKakaoSdk()
    val idToken = requireNotNull(token.idToken) { ERROR_KAKAO_OIDC_DISABLED }
    return KakaoToken(idToken = idToken, accessToken = token.accessToken)
}

private suspend fun loginWithKakaoSdk(): OAuthToken =
    suspendCancellableCoroutine { continuation ->
        val activity = KakaoLoginActivityProvider.requireActivity()
        val callback: (OAuthToken?, Throwable?) -> Unit = callback@{ token, error ->
            if (!continuation.isActive) return@callback
            continuation.resumeWith(
                token?.let(Result.Companion::success)
                    ?: Result.failure(error ?: IllegalStateException("카카오 로그인 결과가 없습니다.")),
            )
        }

        if (!UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
            UserApiClient.instance.loginWithKakaoAccount(activity, callback = callback)
            return@suspendCancellableCoroutine
        }

        UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
            callback(token, error)
        }
    }

private const val ERROR_KAKAO_OIDC_DISABLED = "카카오 OpenID Connect가 활성화되어 있지 않습니다."
