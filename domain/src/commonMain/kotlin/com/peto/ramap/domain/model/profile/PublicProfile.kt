package com.peto.ramap.domain.model.profile

data class PublicProfile(
    val userId: String,
    val nickname: String,
    val bio: String = "",
    val avatarUrl: String? = null,
)
