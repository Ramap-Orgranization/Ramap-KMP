package com.peto.ramap.data.auth

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNotificationCenter

object IosKakaoLoginBridge {
    private const val KAKAO_LOGIN_REQUEST = "KakaoLoginRequest"
    private var continuation: CancellableContinuation<KakaoToken>? = null

    internal suspend fun login(): KakaoToken =
        suspendCancellableCoroutine { continuation ->
            check(this.continuation == null) { "카카오 로그인이 이미 진행 중입니다." }
            this.continuation = continuation
            continuation.invokeOnCancellation { this.continuation = null }
            NSNotificationCenter.defaultCenter.postNotificationName(KAKAO_LOGIN_REQUEST, null)
        }
}
