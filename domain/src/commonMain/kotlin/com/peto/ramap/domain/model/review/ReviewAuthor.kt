package com.peto.ramap.domain.model.review

import kotlin.time.Instant

data class ReviewAuthor(
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val reviewCount: Int = 0,
    val avatarReference: String? = null,
    val avatarExpiresAt: Instant? = null,
)
