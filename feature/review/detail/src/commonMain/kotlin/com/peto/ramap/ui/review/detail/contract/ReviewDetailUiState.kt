package com.peto.ramap.ui.review.detail.contract

import com.peto.ramap.domain.model.review.ReviewDetail
import com.peto.ramap.domain.model.review.comment.ReviewComment
import com.peto.ramap.domain.model.review.comment.ReviewCommentCursor
import com.peto.ramap.domain.model.review.comment.ReviewCommentThread
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

internal data class ReviewDetailUiState(
    val viewerId: String? = null,
    val detail: ReviewDetail? = null,
    val detailFailed: Boolean = false,
    val unavailable: Boolean = false,
    val threads: List<ReviewCommentThread> = emptyList(),
    val nextCursor: ReviewCommentCursor? = null,
    val commentsFailed: Boolean = false,
    val commentsLoaded: Boolean = false,
    val replyFailures: Set<String> = emptySet(),
    val draft: String = "",
    val replyTarget: ReviewComment? = null,
    val commentEdit: ReviewCommentEdit? = null,
    val submitFailed: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<ReviewDetailUiState> {
    val composerDraft: String get() = commentEdit?.draft ?: draft

    val isSubmitting: Boolean get() = loadState.isLoading(ReviewDetailLoadKey.Submit)

    val isRefreshingSession: Boolean get() = loadState.isLoading(ReviewDetailLoadKey.SessionRefresh)

    val isChangingComments: Boolean
        get() = isSubmitting || loadState.isLoading(ReviewDetailLoadKey.CommentMutation)

    override fun withLoadingState(loadState: LoadState): ReviewDetailUiState = copy(loadState = loadState)
}
