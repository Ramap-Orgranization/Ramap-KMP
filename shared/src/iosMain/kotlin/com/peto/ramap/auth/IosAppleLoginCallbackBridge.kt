package com.peto.ramap.auth

import com.peto.ramap.IosAppleLoginBridge

object IosAppleLoginCallbackBridge {
    fun complete(
        idToken: String?,
        nonce: String?,
        errorMessage: String?,
    ) {
        IosAppleLoginBridge.complete(
            idToken = idToken,
            nonce = nonce,
            errorMessage = errorMessage,
        )
    }
}
