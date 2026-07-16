package com.peto.ramap.data.auth

internal data class KakaoToken(
    val idToken: String,
    val accessToken: String,
)

internal expect suspend fun loginWithKakao(): KakaoToken
