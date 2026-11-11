package com.peto.ramap.data.datasource.review

import com.peto.ramap.data.model.ReviewCommentDeletionResponse
import com.peto.ramap.data.model.ReviewCommentPageResponse
import com.peto.ramap.data.model.ReviewCommentReceiptResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.model.review.comment.ReviewCommentCursor
import com.peto.ramap.domain.model.review.comment.ReviewCommentSubmission

internal interface ReviewCommentDataSource {
    suspend fun fetchDetail(reviewId: String): ReviewResponse?

    suspend fun fetchCounts(reviewIds: List<String>): Map<String, Int>

    suspend fun fetchComments(
        reviewId: String,
        parentId: String?,
        cursor: ReviewCommentCursor?,
    ): ReviewCommentPageResponse

    suspend fun submitComment(submission: ReviewCommentSubmission): ReviewCommentReceiptResponse

    suspend fun editComment(
        commentId: String,
        body: String,
        expectedAuthorId: String,
    ): ReviewCommentReceiptResponse

    suspend fun deleteComment(
        commentId: String,
        expectedAuthorId: String,
    ): ReviewCommentDeletionResponse

    suspend fun reportComment(
        commentId: String,
        reason: ReportReason,
        details: String,
        expectedAccountId: String,
    )
}
