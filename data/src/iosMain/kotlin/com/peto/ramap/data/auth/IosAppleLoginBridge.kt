package com.peto.ramap.data.auth

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNotificationCenter

object IosAppleLoginBridge {
    private const val APPLE_LOGIN_REQUEST = "AppleLoginRequest"
    private const val ERROR_MISSING_LOGIN_RESULT = "Apple 로그인 결과가 없습니다."
    private var continuation: CancellableContinuation<AppleToken>? = null

    internal suspend fun login(): AppleToken =
        suspendCancellableCoroutine { continuation ->
            check(this.continuation == null) { "Apple 로그인이 이미 진행 중입니다." }
            this.continuation = continuation
            continuation.invokeOnCancellation {
                if (this.continuation === continuation) {
                    this.continuation = null
                }
            }
            NSNotificationCenter.defaultCenter.postNotificationName(APPLE_LOGIN_REQUEST, null)
        }

    fun complete(
        idToken: String?,
        nonce: String?,
        errorMessage: String?,
    ) {
        val pendingContinuation = continuation ?: return
        continuation = null

        pendingContinuation.resumeWith(
            when {
                errorMessage != null -> Result.failure(IllegalStateException(errorMessage))
                idToken == null || nonce == null -> Result.failure(IllegalStateException(ERROR_MISSING_LOGIN_RESULT))
                else -> Result.success(AppleToken(idToken = idToken, nonce = nonce))
            },
        )
    }
}
