package com.peto.ramap.domain.model.profile

data class AccountProfile(
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val bio: String = "",
    val isPublic: Boolean = false,
    val nicknameChangesRemaining: Int = DAILY_CHANGE_LIMIT,
    val bioChangesRemaining: Int = DAILY_CHANGE_LIMIT,
)

private const val DAILY_CHANGE_LIMIT = 2
