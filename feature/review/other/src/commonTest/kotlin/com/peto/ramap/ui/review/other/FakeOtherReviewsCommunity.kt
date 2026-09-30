package com.peto.ramap.ui.review.other

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.repository.CommunityRepository
import com.peto.ramap.fake.FakeCommunityRepository

class FakeOtherReviewsCommunity : CommunityRepository by FakeCommunityRepository() {
    var profile: PublicProfile? = PublicProfile("author", "라멘팬")
    var profilePrivate = false
    var reviews: List<Review> = emptyList()
    var failNextPage = false
    var failNextBlock = false
    val blockedUserIds = mutableSetOf<String>()
    val requestedProfileUserIds = mutableListOf<String>()
    val requestedOffsets = mutableListOf<Long>()

    override suspend fun fetchProfileAccess(userId: String): RamapResult<ProfileAccess> {
        requestedProfileUserIds += userId
        val target = profile?.takeIf { it.userId == userId } ?: return RamapResult.Success(ProfileAccess.Unavailable)
        val access =
            when {
                userId in blockedUserIds -> ProfileAccess.Blocked(PublicProfile(target.userId, target.nickname))
                profilePrivate -> ProfileAccess.Private
                else -> ProfileAccess.Visible(target)
            }
        return RamapResult.Success(access)
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

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<Review>> {
        requestedOffsets += offset
        if (failNextPage) {
            failNextPage = false
            return RamapResult.Error(RamapError.Unknown(IllegalStateException("page unavailable")))
        }
        return RamapResult.Success(reviews.drop(offset.toInt()).take(20))
    }
}
