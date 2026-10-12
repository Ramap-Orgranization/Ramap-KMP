package com.peto.ramap.domain.model.community

sealed interface ProfileAccess {
    data class Visible(
        val profile: PublicProfile,
        val canReadReviews: Boolean = true,
        val canReadSavedShops: Boolean = true,
        val followState: FollowState = FollowState.NONE,
        val followerCount: Long? = null,
        val followingCount: Long? = null,
        val reviewCount: Long? = null,
        val savedShopCount: Long? = null,
    ) : ProfileAccess

    data class Blocked(
        val profile: PublicProfile,
    ) : ProfileAccess

    data object Private : ProfileAccess

    data object Unavailable : ProfileAccess
}
