package com.peto.ramap.data.auth

internal actual suspend fun loginWithKakao(): KakaoToken = IosKakaoLoginBridge.login()
