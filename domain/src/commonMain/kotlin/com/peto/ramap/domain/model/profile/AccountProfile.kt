package com.peto.ramap.domain.model.profile

data class AccountProfile(
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val bio: String = "",
    val instagramUsername: String = "",
)
