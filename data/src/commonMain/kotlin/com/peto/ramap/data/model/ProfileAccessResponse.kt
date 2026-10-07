package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.FollowState
import com.peto.ramap.domain.model.community.ProfileAccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileAccessResponse(
    val status: String,
    val profile: PublicProfileResponse? = null,
    @SerialName("can_read_reviews") val canReadReviews: Boolean = true,
    @SerialName("can_read_saved_shops") val canReadSavedShops: Boolean = true,
    @SerialName("follow_state") val followState: String = "none",
    @SerialName("follower_count") val followerCount: Long? = null,
    @SerialName("following_count") val followingCount: Long? = null,
    @SerialName("review_count") val reviewCount: Long? = null,
    @SerialName("saved_shop_count") val savedShopCount: Long? = null,
) {
    fun toDomain(): ProfileAccess =
        when (status) {
            VISIBLE ->
                ProfileAccess.Visible(
                    profile = requireNotNull(profile).toDomain(),
                    canReadReviews = canReadReviews,
                    canReadSavedShops = canReadSavedShops,
                    followState = FollowState.valueOf(followState.uppercase()),
                    followerCount = followerCount,
                    followingCount = followingCount,
                    reviewCount = reviewCount.takeIf { canReadReviews },
                    savedShopCount = savedShopCount.takeIf { canReadSavedShops },
                )
            BLOCKED -> ProfileAccess.Blocked(requireNotNull(profile).toDomain())
            PRIVATE -> ProfileAccess.Private
            UNAVAILABLE -> ProfileAccess.Unavailable
            else -> error("Unknown profile access status: $status")
        }

    companion object {
        const val VISIBLE = "visible"
        const val BLOCKED = "blocked"
        const val PRIVATE = "private"
        const val UNAVAILABLE = "unavailable"
    }
}
