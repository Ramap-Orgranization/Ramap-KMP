package com.peto.ramap.data.auth

internal actual suspend fun loginWithApple(): AppleToken = IosAppleLoginBridge.login()
