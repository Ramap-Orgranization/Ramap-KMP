package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.model.review.ReviewDetail
import com.peto.ramap.domain.model.review.comment.ReviewCommentCursor
import com.peto.ramap.domain.model.review.comment.ReviewCommentDeletion
import com.peto.ramap.domain.model.review.comment.ReviewCommentPage
import com.peto.ramap.domain.model.review.comment.ReviewCommentReceipt
import com.peto.ramap.domain.model.review.comment.ReviewCommentSubmission

interface ReviewCommentRepository {
    suspend fun fetchReviewDetail(reviewId: String): RamapResult<ReviewDetail?>

    suspend fun fetchComments(
        reviewId: String,
        parentId: String?,
        cursor: ReviewCommentCursor?,
    ): RamapResult<ReviewCommentPage>

    suspend fun submitComment(submission: ReviewCommentSubmission): RamapResult<ReviewCommentReceipt>

    suspend fun editComment(
        reviewId: String,
        commentId: String,
        body: String,
        expectedAuthorId: String,
    ): RamapResult<ReviewCommentReceipt>

    suspend fun deleteComment(
        reviewId: String,
        commentId: String,
        expectedAuthorId: String,
    ): RamapResult<ReviewCommentDeletion>

    suspend fun reportComment(
        commentId: String,
        reason: ReportReason,
        details: String,
        expectedAccountId: String,
    ): RamapResult<Unit>
}
