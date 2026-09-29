package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.community.ReviewCommunityDataSource
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.repository.ReviewCommunityRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultReviewCommunityRepository(
    private val dataSource: ReviewCommunityDataSource,
    private val changes: ReviewChangeNotifier,
) : ReviewCommunityRepository {
    override fun observeChanges() = changes.events

    override suspend fun fetchMyCommunityProfile(): RamapResult<PublicProfile?> = invokeRequest { dataSource.fetchMyCommunityProfile().toDomain() }

    override suspend fun fetchPublicProfile(userId: String): RamapResult<PublicProfile?> = invokeRequest { dataSource.fetchPublicProfile(userId)?.toDomain() }

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<Review>> = invokeRequest { dataSource.fetchUserReviews(userId, offset).map { it.toDomain() } }

    override suspend fun fetchBlockedUsers(): RamapResult<List<PublicProfile>> = invokeRequest { dataSource.fetchBlockedUsers().map { it.toDomain() } }

    override suspend fun reportReview(
        reviewId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = report("review", reviewId, reason, details)

    override suspend fun reportUser(
        userId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = report("user", userId, reason, details)

    private suspend fun report(
        type: String,
        id: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> =
        invokeRequest {
            require(details.length <= ReportReason.MAX_DETAILS_LENGTH) { "Report details too long" }
            dataSource.report(type, id, reason.name.lowercase(), details.trim())
        }

    override suspend fun blockUser(userId: String): RamapResult<Unit> = changeBlock(userId, true)

    override suspend fun unblockUser(userId: String): RamapResult<Unit> = changeBlock(userId, false)

    private suspend fun changeBlock(
        userId: String,
        blocked: Boolean,
    ): RamapResult<Unit> =
        invokeRequest {
            dataSource.changeBlock(userId, blocked)
            changes.notifyChanged()
        }
}
