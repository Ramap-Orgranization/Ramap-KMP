package com.peto.ramap.domain.model.follow

data class FollowCounts(
    val followers: Long,
    val following: Long,
    val requests: Long,
    val hasUnreadFollowers: Boolean = false,
    val hasUnreadRequests: Boolean = false,
) {
    val hasUnreadConnections: Boolean get() = hasUnread(FollowList.FOLLOWERS) || hasUnread(FollowList.REQUESTS)

    fun hasUnread(list: FollowList): Boolean =
        when (list) {
            FollowList.FOLLOWERS -> followers > 0L && hasUnreadFollowers
            FollowList.REQUESTS -> requests > 0L && hasUnreadRequests
            FollowList.FOLLOWING -> false
        }

    fun count(list: FollowList): Long =
        when (list) {
            FollowList.FOLLOWERS -> followers
            FollowList.FOLLOWING -> following
            FollowList.REQUESTS -> requests
        }

    fun after(action: FollowAction): FollowCounts =
        when (action) {
            FollowAction.APPROVE -> copy(followers = followers + 1L, requests = (requests - 1L).coerceAtLeast(0L))
            FollowAction.REJECT -> copy(requests = (requests - 1L).coerceAtLeast(0L))
            FollowAction.REMOVE -> copy(followers = (followers - 1L).coerceAtLeast(0L))
            FollowAction.UNFOLLOW -> copy(following = (following - 1L).coerceAtLeast(0L))
            FollowAction.FOLLOW -> this
        }
}
