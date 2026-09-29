package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.repository.ReviewCommunityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeReviewCommunityRepository(
    var blockedUsersResult: RamapResult<List<PublicProfile>> = RamapResult.Success(emptyList()),
) : ReviewCommunityRepository {
    override fun observeChanges(): Flow<Unit> = emptyFlow()

    override suspend fun fetchMyCommunityProfile(): RamapResult<PublicProfile?> = RamapResult.Success(PublicProfile("me", "나"))

    override suspend fun fetchPublicProfile(userId: String): RamapResult<PublicProfile?> = RamapResult.Success(null)

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<Review>> = RamapResult.Success(emptyList())

    override suspend fun fetchBlockedUsers(): RamapResult<List<PublicProfile>> = blockedUsersResult

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

    override suspend fun unblockUser(userId: String): RamapResult<Unit> = RamapResult.Success(Unit)
}
