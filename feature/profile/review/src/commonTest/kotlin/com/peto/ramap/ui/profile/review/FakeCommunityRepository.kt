package com.peto.ramap.ui.profile.review

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.repository.ReviewCommunityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class FakeCommunityRepository(
    private val publicProfiles: Map<String, PublicProfile> = emptyMap(),
) : ReviewCommunityRepository {
    val blocked = linkedSetOf<String>()
    val reportedReviews = mutableListOf<String>()
    var reportError: RamapError? = null

    override fun observeChanges(): Flow<Unit> = emptyFlow()

    override suspend fun fetchMyCommunityProfile(): RamapResult<PublicProfile?> = RamapResult.Success(PublicProfile("me", "나"))

    override suspend fun fetchPublicProfile(userId: String) = RamapResult.Success(publicProfiles[userId])

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ) = RamapResult.Success(emptyList<Review>())

    override suspend fun fetchBlockedUsers() = RamapResult.Success(blocked.map { PublicProfile(it, it) })

    override suspend fun reportReview(
        reviewId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = reportError?.let { RamapResult.Error(it) } ?: RamapResult.Success(Unit).also { reportedReviews += reviewId }

    override suspend fun reportUser(
        userId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun blockUser(userId: String): RamapResult<Unit> = RamapResult.Success(Unit).also { blocked += userId }

    override suspend fun unblockUser(userId: String): RamapResult<Unit> = RamapResult.Success(Unit).also { blocked -= userId }
}
