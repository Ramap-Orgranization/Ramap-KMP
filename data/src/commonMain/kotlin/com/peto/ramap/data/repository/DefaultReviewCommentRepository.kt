package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.review.ReviewCommentDataSource
import com.peto.ramap.data.store.DefaultReviewChangeStore
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.model.review.ReviewDetail
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.comment.ReviewCommentBody
import com.peto.ramap.domain.model.review.comment.ReviewCommentCursor
import com.peto.ramap.domain.model.review.comment.ReviewCommentSubmission
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ReviewCommentRepository
import com.peto.ramap.network.execute.invokeRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultReviewCommentRepository(
    private val dataSource: ReviewCommentDataSource,
    private val changes: DefaultReviewChangeStore,
    private val shops: RamenShopRepository,
) : ReviewCommentRepository {
    private val mutations = Mutex()

    override suspend fun fetchReviewDetail(reviewId: String) =
        invokeRequest {
            val detail = dataSource.fetchDetail(reviewId) ?: return@invokeRequest null
            val review = detail.toDomain()
            val shop =
                when (val result = shops.fetchRamenShops(setOf(review.shopId))) {
                    is RamapResult.Success -> result.data[review.shopId]
                    is RamapResult.Error -> null
                }
            ReviewDetail(
                review = review,
                shopName = detail.shopName,
                canLike = detail.canLike,
                canWriteComments = detail.canWriteComments,
                shop = shop,
                canShare = detail.canShare,
                reviewNotificationsEnabled = detail.reviewNotificationsEnabled,
            )
        }

    override suspend fun fetchComments(
        reviewId: String,
        parentId: String?,
        cursor: ReviewCommentCursor?,
    ) = invokeRequest { dataSource.fetchComments(reviewId, parentId, cursor).toDomain() }

    override suspend fun submitComment(submission: ReviewCommentSubmission) =
        mutations.withLock {
            invokeRequest {
                require(ReviewCommentBody.isValid(submission.body)) { "Invalid review comment body" }
                val epoch = changes.synchronizeSession()
                check(changes.journal.value.userId == submission.expectedAuthorId) { "Review comment session changed" }
                dataSource.submitComment(submission.copy(body = submission.body.trim())).toDomain().also { receipt ->
                    changes.publish(ReviewChange.Commented(submission.reviewId, receipt.commentCount), epoch)
                }
            }
        }

    override suspend fun editComment(
        reviewId: String,
        commentId: String,
        body: String,
        expectedAuthorId: String,
    ) = mutations.withLock {
        invokeRequest {
            require(ReviewCommentBody.isValid(body)) { "Invalid review comment body" }
            val epoch = changes.synchronizeSession()
            check(changes.journal.value.userId == expectedAuthorId) { "Review comment session changed" }
            dataSource.editComment(commentId, body.trim(), expectedAuthorId).toDomain().also { receipt ->
                changes.publish(ReviewChange.Commented(reviewId, receipt.commentCount), epoch)
            }
        }
    }

    override suspend fun deleteComment(
        reviewId: String,
        commentId: String,
        expectedAuthorId: String,
    ) = mutations.withLock {
        invokeRequest {
            val epoch = changes.synchronizeSession()
            check(changes.journal.value.userId == expectedAuthorId) { "Review comment session changed" }
            dataSource.deleteComment(commentId, expectedAuthorId).toDomain().also { deletion ->
                changes.publish(ReviewChange.Commented(reviewId, deletion.commentCount), epoch)
            }
        }
    }

    override suspend fun reportComment(
        commentId: String,
        reason: ReportReason,
        details: String,
        expectedAccountId: String,
    ) = invokeRequest { dataSource.reportComment(commentId, reason, details.trim(), expectedAccountId) }
}
