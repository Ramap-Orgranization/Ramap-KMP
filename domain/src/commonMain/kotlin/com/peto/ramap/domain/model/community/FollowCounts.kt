package com.peto.ramap.domain.model.community

data class FollowCounts(
    val followers: Long,
    val following: Long,
    val requests: Long,
) {
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
