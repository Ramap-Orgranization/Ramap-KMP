package com.peto.ramap.domain.model.community

import kotlin.time.Instant

data class ReviewAuthor(
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val reviewCount: Int = 0,
    val avatarReference: String? = null,
    val avatarExpiresAt: Instant? = null,
)
