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
    @SerialName("instagram_username")
    val instagramUsername: String = "",
)
