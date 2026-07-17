package com.peto.ramap

import com.peto.ramap.data.auth.IosKakaoLoginBridge as DataIosKakaoLoginBridge

object IosKakaoLoginBridge {
    fun complete(
        idToken: String?,
        accessToken: String?,
        errorMessage: String?,
    ) {
        DataIosKakaoLoginBridge.complete(idToken, accessToken, errorMessage)
    }
}
