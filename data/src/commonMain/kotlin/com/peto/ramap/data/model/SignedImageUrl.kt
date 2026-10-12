package com.peto.ramap.data.model

import kotlin.time.Instant

internal data class SignedImageUrl(
    val url: String,
    val expiresAt: Instant,
)
