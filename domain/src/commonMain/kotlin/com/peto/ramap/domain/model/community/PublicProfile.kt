package com.peto.ramap.domain.model.community

data class PublicProfile(
    val userId: String,
    val nickname: String,
    val bio: String = "",
    val avatarUrl: String? = null,
)
