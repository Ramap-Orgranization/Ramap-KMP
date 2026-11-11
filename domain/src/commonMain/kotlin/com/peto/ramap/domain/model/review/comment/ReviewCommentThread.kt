package com.peto.ramap.domain.model.review.comment

data class ReviewCommentThread(
    val comment: ReviewComment,
    val replies: List<ReviewComment>,
    val nextReplyCursor: ReviewCommentCursor?,
) {
    val hasMoreReplies: Boolean
        get() = nextReplyCursor != null || (comment.isDeleted && replies.isEmpty())
}
