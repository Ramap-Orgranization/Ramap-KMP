package com.peto.ramap.domain.model.community

data class ReviewAuthor(
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val reviewCount: Int = 0,
)
