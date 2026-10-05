package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.BlockedUser
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.PublicSavedShopsPage
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeCommunityRepository(
    var blockedUsersResult: RamapResult<List<BlockedUser>> = RamapResult.Success(emptyList()),
) : CommunityRepository {
    var unblockResult: RamapResult<Unit> = RamapResult.Success(Unit)
    val unblockedUserIds = mutableListOf<String>()

    override fun observeChanges(): Flow<Unit> = emptyFlow()

    override suspend fun fetchMyCommunityProfile(): RamapResult<PublicProfile?> = RamapResult.Success(PublicProfile("me", "나"))

    override suspend fun fetchProfileAccess(userId: String): RamapResult<ProfileAccess> = RamapResult.Success(ProfileAccess.Unavailable)

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<ProfileReview>> = RamapResult.Success(emptyList())

    override suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): RamapResult<PublicSavedShopsPage> = RamapResult.Success(PublicSavedShopsPage(ProfileAccess.Unavailable, emptyList()))

    override suspend fun fetchBlockedUsers(): RamapResult<List<BlockedUser>> = blockedUsersResult

    override suspend fun reportReview(
        reviewId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun reportUser(
        userId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun blockUser(userId: String): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun unblockUser(userId: String): RamapResult<Unit> {
        unblockedUserIds += userId
        if (unblockResult is RamapResult.Success) {
            val users = (blockedUsersResult as? RamapResult.Success)?.data.orEmpty()
            blockedUsersResult = RamapResult.Success(users.filterNot { it.profile.userId == userId })
        }
        return unblockResult
    }
}
