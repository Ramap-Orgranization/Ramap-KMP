package com.peto.ramap.data.model

import com.peto.ramap.domain.model.follow.FollowCounts
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FollowCountsResponse(
    val followers: Long,
    val following: Long,
    val requests: Long,
    @SerialName("has_unread_followers")
    val hasUnreadFollowers: Boolean = false,
    @SerialName("has_unread_requests")
    val hasUnreadRequests: Boolean = false,
) {
    fun toDomain(): FollowCounts =
        FollowCounts(
            followers = followers,
            following = following,
            requests = requests,
            hasUnreadFollowers = hasUnreadFollowers,
            hasUnreadRequests = hasUnreadRequests,
        )
}
