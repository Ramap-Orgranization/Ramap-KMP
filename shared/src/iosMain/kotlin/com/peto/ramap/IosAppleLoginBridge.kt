package com.peto.ramap

import com.peto.ramap.data.auth.IosAppleLoginBridge as DataIosAppleLoginBridge

object IosAppleLoginBridge {
    fun complete(
        idToken: String?,
        nonce: String?,
        errorMessage: String?,
    ) {
        DataIosAppleLoginBridge.complete(idToken, nonce, errorMessage)
    }
}
