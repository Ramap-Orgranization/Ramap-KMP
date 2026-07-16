package com.peto.ramap.data.auth

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNotificationCenter

object IosKakaoLoginBridge {
    private const val KAKAO_LOGIN_REQUEST = "KakaoLoginRequest"
    private const val ERROR_MISSING_LOGIN_RESULT = "카카오 로그인 결과가 없습니다."
    private var continuation: CancellableContinuation<KakaoToken>? = null

    internal suspend fun login(): KakaoToken =
        suspendCancellableCoroutine { continuation ->
            check(this.continuation == null) { "카카오 로그인이 이미 진행 중입니다." }
            this.continuation = continuation
            continuation.invokeOnCancellation {
                if (this.continuation === continuation) {
                    this.continuation = null
                }
            }
            NSNotificationCenter.defaultCenter.postNotificationName(KAKAO_LOGIN_REQUEST, null)
        }

    fun complete(
        idToken: String?,
        accessToken: String?,
        errorMessage: String?,
    ) {
        val pendingContinuation = continuation ?: return
        continuation = null

        pendingContinuation.resumeWith(
            when {
                errorMessage != null -> Result.failure(IllegalStateException(errorMessage))
                idToken == null || accessToken == null -> Result.failure(IllegalStateException(ERROR_MISSING_LOGIN_RESULT))
                else -> Result.success(KakaoToken(idToken, accessToken))
            },
        )
    }
}
