package com.peto.ramap.auth

import com.peto.ramap.IosKakaoLoginBridge

object IosKakaoLoginCallbackBridge {
    fun complete(
        idToken: String?,
        accessToken: String?,
        errorMessage: String?,
    ) {
        IosKakaoLoginBridge.complete(
            idToken = idToken,
            accessToken = accessToken,
            errorMessage = errorMessage,
        )
    }
}
