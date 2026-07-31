package com.peto.ramap.data.auth

internal data class AppleToken(
    val idToken: String,
    val nonce: String,
)
