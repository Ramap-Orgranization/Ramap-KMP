package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.PublicProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PublicProfileResponse(
    @SerialName("user_id")
    val userId: String,
    val nickname: String,
    val bio: String = "",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
) {
    fun toDomain(): PublicProfile = PublicProfile(userId, nickname, bio, avatarUrl)
}
