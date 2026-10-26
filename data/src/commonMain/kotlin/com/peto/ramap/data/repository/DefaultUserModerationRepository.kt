package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.moderation.UserModerationDataSource
import com.peto.ramap.data.store.DefaultReviewChangeStore
import com.peto.ramap.domain.model.moderation.BlockedUser
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.repository.UserModerationRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultUserModerationRepository(
    private val dataSource: UserModerationDataSource,
    private val changes: DefaultReviewChangeStore,
) : UserModerationRepository {
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
            val epoch = changes.synchronizeSession()
            dataSource.changeBlock(userId, blocked)
            changes.publish(ReviewChange.AccessChanged(userId), epoch)
        }

    private companion object {
        const val REPORT_TYPE_REVIEW = "review"
        const val REPORT_TYPE_USER = "user"
        const val ERROR_REPORT_DETAILS_TOO_LONG = "Report details too long"
    }
}
