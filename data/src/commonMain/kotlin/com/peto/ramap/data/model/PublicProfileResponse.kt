package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.PublicProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class PublicProfileResponse(
    @SerialName("user_id")
    val userId: String,
    val nickname: String,
    val bio: String = "",
    @SerialName("avatar_path")
    val avatarPath: String? = null,
    @Transient
    val avatarUrl: String? = null,
) {
    fun toDomain(): PublicProfile = PublicProfile(userId, nickname, bio, avatarUrl)
}
