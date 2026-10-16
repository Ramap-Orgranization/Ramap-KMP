package com.peto.ramap.data.auth

import com.peto.ramap.IosAppleLoginBridge

internal actual suspend fun loginWithApple(): AppleToken = IosAppleLoginBridge.login()
