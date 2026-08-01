package com.peto.ramap.data.auth

internal actual suspend fun loginWithApple(): AppleToken = error("Apple 로그인은 iOS에서만 지원합니다.")
