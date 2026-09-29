package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.PublicProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class MyCommunityProfileResponse(
    @SerialName("user_id")
    val userId: String? = null,
    val nickname: String? = null,
    val bio: String = "",
    @SerialName("avatar_path")
    val avatarPath: String? = null,
    @Transient val avatarUrl: String? = null,
) {
    fun toDomain(): PublicProfile? =
        if (userId != null && nickname != null) {
            PublicProfile(userId, nickname, bio, avatarUrl)
        } else {
            null
        }
}
