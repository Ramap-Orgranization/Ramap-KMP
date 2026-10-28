package com.peto.ramap.ui.review.other

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.follow.FollowState
import com.peto.ramap.domain.model.profile.ProfileAccess
import com.peto.ramap.domain.model.profile.PublicProfile
import com.peto.ramap.domain.model.profile.PublicSavedShopsPage
import com.peto.ramap.domain.model.review.ProfileReview
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.repository.UserModerationRepository
import com.peto.ramap.domain.repository.UserProfileRepository
import com.peto.ramap.fake.FakeUserModerationRepository
import com.peto.ramap.fake.FakeUserProfileRepository
import kotlinx.coroutines.CompletableDeferred

class FakeOtherReviewsRepositories :
    UserProfileRepository by FakeUserProfileRepository(),
    UserModerationRepository by FakeUserModerationRepository() {
    var profile: PublicProfile? = PublicProfile("author", "라멘팬")
    var profilePrivate = false
    var canReadReviews = true
    var canReadSavedShops = true
    var followState = FollowState.NONE
    var followerCount = 0L
    var followingCount = 0L
    var profileAccessPending: CompletableDeferred<RamapResult<ProfileAccess>>? = null
    var reviews: List<ProfileReview> = emptyList()
    var savedShops: List<RamenShop> = emptyList()
    var reviewCount: Long? = null
    var savedShopCount: Long? = null
    var failNextPage = false
    var failNextSavedShops = false
    var failNextBlock = false
    val blockedUserIds = mutableSetOf<String>()
    val requestedProfileUserIds = mutableListOf<String>()
    val requestedOffsets = mutableListOf<Long>()
    val requestedSavedShopOffsets = mutableListOf<Long>()

    override suspend fun fetchProfileAccess(userId: String): RamapResult<ProfileAccess> {
        requestedProfileUserIds += userId
        return profileAccessPending?.await() ?: RamapResult.Success(profileAccess(userId))
    }

    private fun profileAccess(userId: String): ProfileAccess {
        val target = profile?.takeIf { it.userId == userId } ?: return ProfileAccess.Unavailable
        return when {
            userId in blockedUserIds -> ProfileAccess.Blocked(PublicProfile(target.userId, target.nickname))
            profilePrivate -> ProfileAccess.Private
            else ->
                ProfileAccess.Visible(
                    profile = target,
                    canReadReviews = canReadReviews,
                    canReadSavedShops = canReadSavedShops,
                    followState = followState,
                    followerCount = followerCount,
                    followingCount = followingCount,
                    reviewCount = reviewCount,
                    savedShopCount = savedShopCount,
                )
        }
    }

    override suspend fun blockUser(userId: String): RamapResult<Unit> {
        if (failNextBlock) {
            failNextBlock = false
            return RamapResult.Error(RamapError.Unknown(IllegalStateException("block unavailable")))
        }
        blockedUserIds += userId
        return RamapResult.Success(Unit)
    }

    override suspend fun unblockUser(userId: String): RamapResult<Unit> {
        blockedUserIds -= userId
        return RamapResult.Success(Unit)
    }

    override suspend fun fetchProfileReviewsPage(
        userId: String,
        offset: Long,
    ): RamapResult<com.peto.ramap.domain.model.profile.ProfileReviewsPage> {
        val result = fetchUserReviews(userId, offset)
        return when (result) {
            is RamapResult.Error -> result
            is RamapResult.Success ->
                RamapResult.Success(
                    com.peto.ramap.domain.model.profile.ProfileReviewsPage(
                        profileAccess(userId),
                        result.data,
                    ),
                )
        }
    }

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<ProfileReview>> {
        requestedOffsets += offset
        if (failNextPage) {
            failNextPage = false
            return RamapResult.Error(RamapError.Unknown(IllegalStateException("page unavailable")))
        }
        return RamapResult.Success(reviews.drop(offset.toInt()).take(20))
    }

    override suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): RamapResult<PublicSavedShopsPage> {
        requestedSavedShopOffsets += offset
        if (failNextSavedShops) {
            failNextSavedShops = false
            return RamapResult.Error(RamapError.Unknown(IllegalStateException("saved shops unavailable")))
        }
        val access = profileAccess(userId)
        return RamapResult.Success(PublicSavedShopsPage(access, savedShops.drop(offset.toInt()).take(20)))
    }
}
