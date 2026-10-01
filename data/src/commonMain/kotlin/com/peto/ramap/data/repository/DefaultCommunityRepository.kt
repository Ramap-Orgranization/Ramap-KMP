package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.community.CommunityDataSource
import com.peto.ramap.domain.model.community.BlockedUser
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.repository.CommunityRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultCommunityRepository(
    private val dataSource: CommunityDataSource,
    private val changes: ReviewChangeNotifier,
) : CommunityRepository {
    override fun observeChanges() = changes.events

    override suspend fun fetchMyCommunityProfile(): RamapResult<PublicProfile?> = invokeRequest { dataSource.fetchMyCommunityProfile().toDomain() }

    override suspend fun fetchProfileAccess(userId: String): RamapResult<ProfileAccess> = invokeRequest { dataSource.fetchProfileAccess(userId).toDomain() }

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<Review>> = invokeRequest { dataSource.fetchUserReviews(userId, offset).map { it.toDomain() } }

    override suspend fun fetchBlockedUsers(): RamapResult<List<BlockedUser>> = invokeRequest { dataSource.fetchBlockedUsers().map { it.toDomain() } }

    override suspend fun reportReview(
        reviewId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = report(REPORT_TYPE_REVIEW, reviewId, reason, details)

    override suspend fun reportUser(
        userId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = report(REPORT_TYPE_USER, userId, reason, details)

    private suspend fun report(
        type: String,
        id: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> =
        invokeRequest {
            require(details.length <= ReportReason.MAX_DETAILS_LENGTH) { ERROR_REPORT_DETAILS_TOO_LONG }
            dataSource.report(type, id, reason.name.lowercase(), details.trim())
        }

    override suspend fun blockUser(userId: String): RamapResult<Unit> = changeBlock(userId, blocked = true)

    override suspend fun unblockUser(userId: String): RamapResult<Unit> = changeBlock(userId, blocked = false)

    private suspend fun changeBlock(
        userId: String,
        blocked: Boolean,
    ): RamapResult<Unit> =
        invokeRequest {
            dataSource.changeBlock(userId, blocked)
            changes.notifyChanged()
        }

    private companion object {
        const val REPORT_TYPE_REVIEW = "review"
        const val REPORT_TYPE_USER = "user"
        const val ERROR_REPORT_DETAILS_TOO_LONG = "Report details too long"
    }
}
