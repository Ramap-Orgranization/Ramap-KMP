package com.peto.ramap.domain.model.review

import kotlin.time.Instant

data class MyReviewPhoto(
    val url: String?,
    val reference: String,
    val expiresAt: Instant? = null,
)
