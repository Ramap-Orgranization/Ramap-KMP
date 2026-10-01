package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.BlockedUser
import com.peto.ramap.domain.model.community.PublicProfile
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Instant

@Serializable
internal data class BlockedUserResponse(
    @SerialName("user_id")
    val userId: String,
    val nickname: String,
    @SerialName("avatar_path")
    val avatarPath: String? = null,
    @SerialName("blocked_at")
    val blockedAt: String,
    @Transient
    val avatarUrl: String? = null,
) {
    fun toDomain(): BlockedUser =
        BlockedUser(
            profile = PublicProfile(userId = userId, nickname = nickname, avatarUrl = avatarUrl),
            blockedOn = Instant.parse(blockedAt).toLocalDateTime(TimeZone.of("Asia/Seoul")).date,
        )
}
