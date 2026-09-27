package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileResponse(
    @SerialName("user_id")
    val userId: String,
    val nickname: String,
    @SerialName("avatar_path")
    val avatarPath: String? = null,
    val bio: String = "",
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("nickname_changes_remaining") val nicknameChangesRemaining: Int = DAILY_CHANGE_LIMIT,
    @SerialName("bio_changes_remaining") val bioChangesRemaining: Int = DAILY_CHANGE_LIMIT,
)

private const val DAILY_CHANGE_LIMIT = 2
