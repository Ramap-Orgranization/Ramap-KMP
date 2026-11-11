package com.peto.ramap.ui.review.detail.contract

import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.model.review.change.ReviewChangeJournal
import com.peto.ramap.domain.model.review.comment.ReviewComment
import com.peto.ramap.ui.base.Intent

internal sealed interface ReviewDetailIntent : Intent {
    data object RetryDetail : ReviewDetailIntent

    data object RetryComments : ReviewDetailIntent

    data object LoadMoreComments : ReviewDetailIntent

    data class LoadMoreReplies(
        val parentId: String,
    ) : ReviewDetailIntent

    data class ChangeCommentDraft(
        val text: String,
        val editingCommentId: String? = null,
    ) : ReviewDetailIntent

    data class StartCommentEdit(
        val commentId: String,
    ) : ReviewDetailIntent

    data object CancelCommentEdit : ReviewDetailIntent

    data class SaveCommentEdit(
        val commentId: String,
    ) : ReviewDetailIntent

    data class DeleteComment(
        val commentId: String,
    ) : ReviewDetailIntent

    data class ReplyTo(
        val comment: ReviewComment,
    ) : ReviewDetailIntent

    data object CancelReply : ReviewDetailIntent

    data object Submit : ReviewDetailIntent

    data object ToggleLike : ReviewDetailIntent

    data object Delete : ReviewDetailIntent

    data object ToggleNotifications : ReviewDetailIntent

    data class Report(
        val reason: ReportReason,
        val details: String,
        val commentId: String? = null,
    ) : ReviewDetailIntent

    data class JournalUpdated(
        val journal: ReviewChangeJournal,
    ) : ReviewDetailIntent
}
