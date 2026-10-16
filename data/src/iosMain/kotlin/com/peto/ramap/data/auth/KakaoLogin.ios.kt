package com.peto.ramap.data.auth

import com.peto.ramap.IosKakaoLoginBridge

internal actual suspend fun loginWithKakao(): KakaoToken = IosKakaoLoginBridge.login()
