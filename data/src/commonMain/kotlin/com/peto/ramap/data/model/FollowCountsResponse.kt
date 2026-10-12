package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.FollowCounts
import kotlinx.serialization.Serializable

@Serializable
internal data class FollowCountsResponse(
    val followers: Long,
    val following: Long,
    val requests: Long,
) {
    fun toDomain(): FollowCounts = FollowCounts(followers, following, requests)
}
