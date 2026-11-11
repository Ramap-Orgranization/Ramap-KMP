package com.peto.ramap.ui.review.detail

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeJournal
import com.peto.ramap.domain.model.review.comment.ReviewComment
import com.peto.ramap.domain.model.review.comment.ReviewCommentBody
import com.peto.ramap.domain.model.review.comment.ReviewCommentDeletion
import com.peto.ramap.domain.model.review.comment.ReviewCommentSubmission
import com.peto.ramap.domain.model.review.comment.ReviewCommentThread
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ReviewCommentRepository
import com.peto.ramap.domain.repository.ReviewNotificationRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.repository.UserModerationRepository
import com.peto.ramap.domain.store.ReviewChangeStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.review.detail.contract.ReviewCommentEdit
import com.peto.ramap.ui.review.detail.contract.ReviewDetailEffect
import com.peto.ramap.ui.review.detail.contract.ReviewDetailIntent
import com.peto.ramap.ui.review.detail.contract.ReviewDetailLoadKey
import com.peto.ramap.ui.review.detail.contract.ReviewDetailUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.login_required_review_like_description
import ramap.shared.generated.resources.login_required_review_report_description
import ramap.shared.generated.resources.personalization_update_failure_message
import ramap.shared.generated.resources.review_comment_delete_failed
import ramap.shared.generated.resources.review_comment_edit_failed
import ramap.shared.generated.resources.review_comment_report_login_required
import ramap.shared.generated.resources.review_delete_failed
import ramap.shared.generated.resources.review_like_failed
import ramap.shared.generated.resources.review_report_failed
import kotlin.uuid.Uuid

internal class ReviewDetailViewModel(
    private val reviewId: String,
    private val showDetail: Boolean,
    private val comments: ReviewCommentRepository,
    private val reviews: ReviewRepository,
    private val moderation: UserModerationRepository,
    private val login: LoginRepository,
    private val changes: ReviewChangeStore,
    private val notifications: ReviewNotificationRepository,
) : BaseViewModel<ReviewDetailUiState, ReviewDetailIntent, ReviewDetailEffect>(
        ReviewDetailUiState(viewerId = changes.journal.value.userId),
    ) {
    private var epoch = changes.journal.value.epoch
    private var revision = changes.journal.value.revision
    private var requestId: String? = null

    init {
        dispatch(ReviewDetailIntent.RetryDetail)
        viewModelScope.launch {
            changes.journal.collect { dispatch(ReviewDetailIntent.JournalUpdated(it)) }
        }
    }

    override suspend fun handleIntent(intent: ReviewDetailIntent) {
        if (currentState.isRefreshingSession && intent !is ReviewDetailIntent.JournalUpdated) return
        when (intent) {
            ReviewDetailIntent.RetryDetail -> reload()
            ReviewDetailIntent.RetryComments -> fetchComments(append = false)
            ReviewDetailIntent.LoadMoreComments -> fetchComments(append = true)
            is ReviewDetailIntent.LoadMoreReplies -> fetchReplies(intent.parentId)
            is ReviewDetailIntent.ChangeCommentDraft -> changeDraft(intent)
            is ReviewDetailIntent.StartCommentEdit -> startCommentEdit(intent.commentId)
            ReviewDetailIntent.CancelCommentEdit -> cancelCommentEdit()
            is ReviewDetailIntent.SaveCommentEdit -> saveCommentEdit(intent.commentId)
            is ReviewDetailIntent.DeleteComment -> deleteComment(intent.commentId)
            is ReviewDetailIntent.ReplyTo -> changeReplyTarget(intent.comment)
            ReviewDetailIntent.CancelReply -> changeReplyTarget(null)
            ReviewDetailIntent.Submit -> submitComment()
            ReviewDetailIntent.ToggleLike -> toggleLike()
            ReviewDetailIntent.Delete -> deleteReview()
            ReviewDetailIntent.ToggleNotifications -> toggleNotifications()
            is ReviewDetailIntent.Report -> report(intent)
            is ReviewDetailIntent.JournalUpdated -> applyJournal(intent.journal)
        }
    }

    private fun reload(keepVisibleContent: Boolean = false) {
        cancelReads()
        if (currentState.replyTarget != null) requestId = null
        reduce {
            copy(
                detail = detail.takeIf { keepVisibleContent },
                detailFailed = false,
                unavailable = false,
                threads = if (keepVisibleContent) threads else emptyList(),
                nextCursor = null,
                commentsFailed = false,
                commentsLoaded = keepVisibleContent && commentsLoaded,
                replyFailures = emptySet(),
                replyTarget = null,
                commentEdit = null,
            )
        }
        if (!showDetail) {
            fetchComments(append = false)
            return
        }
        val requestEpoch = epoch
        launchResultTask(
            taskKey = DETAIL_TASK,
            loadKey = if (keepVisibleContent) ReviewDetailLoadKey.SessionRefresh else ReviewDetailLoadKey.Detail,
            request = { comments.fetchReviewDetail(reviewId) },
            onSuccess = { detail ->
                if (isCurrent(requestEpoch)) {
                    if (detail == null) {
                        markUnavailable()
                    } else {
                        reduce { copy(detail = detail) }
                        fetchComments(append = false, refreshSession = keepVisibleContent)
                    }
                }
            },
            onError = {
                if (isCurrent(requestEpoch)) {
                    reduce { copy(detail = null, threads = emptyList(), commentsLoaded = false, detailFailed = true) }
                }
            },
        )
    }

    private fun fetchComments(
        append: Boolean,
        refreshSession: Boolean = false,
    ) {
        if (currentState.isChangingComments || (showDetail && currentState.detail == null)) return
        val cursor = if (append) currentState.nextCursor ?: return else null
        if (!append) currentState.threads.forEach { cancelTask(replyTask(it.comment.id)) }
        val requestEpoch = epoch
        launchResultTask(
            taskKey = COMMENTS_TASK,
            loadKey = if (refreshSession) ReviewDetailLoadKey.SessionRefresh else ReviewDetailLoadKey.Comments,
            policy = if (append) TaskPolicy.IgnoreNew else TaskPolicy.CancelPrevious,
            onStart = { copy(commentsFailed = false, replyFailures = if (append) replyFailures else emptySet()) },
            request = { comments.fetchComments(reviewId, parentId = null, cursor = cursor) },
            onSuccess = { page ->
                if (isCurrent(requestEpoch)) {
                    reduce {
                        val merged = (if (append) threads + page.threads else page.threads).distinctBy { it.comment.id }
                        copy(threads = merged, nextCursor = page.nextCursor, commentsLoaded = true).withCount(page.commentCount)
                    }
                }
            },
            onError = { error ->
                if (isCurrent(requestEpoch)) {
                    if (error.deniesAccess()) {
                        markUnavailable()
                    } else {
                        reduce {
                            copy(
                                threads = if (refreshSession) emptyList() else threads,
                                commentsLoaded = !refreshSession && commentsLoaded,
                                commentsFailed = true,
                            )
                        }
                    }
                }
            },
        )
    }

    private fun fetchReplies(parentId: String) {
        if (currentState.isChangingComments) return
        val thread = currentState.threads.firstOrNull { it.comment.id == parentId } ?: return
        if (!thread.hasMoreReplies) return
        val cursor = thread.nextReplyCursor
        val requestEpoch = epoch
        launchResultTask(
            taskKey = replyTask(parentId),
            loadKey = ReviewDetailLoadKey.Replies(parentId),
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(replyFailures = replyFailures - parentId) },
            request = { comments.fetchComments(reviewId, parentId, cursor) },
            onSuccess = { page ->
                if (isCurrent(requestEpoch)) {
                    reduce {
                        val updated =
                            threads.mapNotNull {
                                if (it.comment.id != parentId) {
                                    it
                                } else if (it.comment.isDeleted && it.replies.isEmpty() && page.threads.isEmpty()) {
                                    null
                                } else {
                                    it.copy(
                                        replies = mergeReplies(it.replies, page.threads.map { row -> row.comment }),
                                        nextReplyCursor = page.nextCursor,
                                    )
                                }
                            }
                        copy(threads = updated).withCount(page.commentCount)
                    }
                }
            },
            onError = { error ->
                if (isCurrent(requestEpoch)) {
                    if (error.deniesAccess()) reload() else reduce { copy(replyFailures = replyFailures + parentId) }
                }
            },
        )
    }

    private fun changeDraft(intent: ReviewDetailIntent.ChangeCommentDraft) {
        val state = currentState
        val edit = state.commentEdit
        if (state.isChangingComments || intent.editingCommentId != edit?.commentId || intent.text == state.composerDraft) return
        if (edit != null) {
            reduce { copy(commentEdit = edit.copy(draft = intent.text)) }
            return
        }
        requestId = null
        reduce { copy(draft = intent.text, submitFailed = false) }
    }

    private fun changeReplyTarget(comment: ReviewComment?) {
        if (currentState.isChangingComments) return
        if (comment != null && (comment.isDeleted || comment.parentId != null || currentState.detail?.canWriteComments != true)) return
        if (comment?.id == currentState.replyTarget?.id) {
            if (comment != null) cancelCommentEdit()
            return
        }
        requestId = null
        reduce { copy(replyTarget = comment, commentEdit = null, submitFailed = false) }
    }

    private fun submitComment() {
        val state = currentState
        val viewerId = state.viewerId ?: return
        if (state.commentEdit != null ||
            state.detail?.canWriteComments != true ||
            !state.commentsLoaded ||
            !ReviewCommentBody.isValid(state.draft) ||
            state.isChangingComments
        ) {
            return
        }
        val requestEpoch = epoch
        val submission =
            ReviewCommentSubmission(
                reviewId = reviewId,
                parentId = state.replyTarget?.id,
                body = state.draft,
                requestId = requestId ?: Uuid.random().toString().also { requestId = it },
                expectedAuthorId = viewerId,
            )
        cancelReads()
        launchResultTask(
            taskKey = SUBMIT_TASK,
            loadKey = ReviewDetailLoadKey.Submit,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(submitFailed = false) },
            request = { comments.submitComment(submission) },
            onSuccess = { receipt ->
                if (isCurrent(requestEpoch)) {
                    requestId = null
                    reduce {
                        copy(
                            draft = "",
                            replyTarget = null,
                            threads = if (receipt.comment.isDeleted) threads else insertSubmittedComment(receipt.comment),
                        ).withCount(receipt.commentCount)
                    }
                }
            },
            onError = { error ->
                if (isCurrent(requestEpoch)) {
                    reduce { copy(submitFailed = true) }
                    if (error.deniesAccess()) reload()
                }
            },
        )
    }

    private fun startCommentEdit(commentId: String) {
        val state = currentState
        if (state.isChangingComments ||
            state.viewerId == null ||
            state.detail?.canWriteComments != true ||
            state.commentEdit?.commentId == commentId
        ) {
            return
        }
        val comment = managedComment(commentId) ?: return
        reduce { copy(commentEdit = ReviewCommentEdit(comment.id, comment.body)) }
    }

    private fun cancelCommentEdit() {
        if (currentState.isChangingComments || currentState.commentEdit == null) return
        reduce { copy(commentEdit = null) }
    }

    private fun saveCommentEdit(commentId: String) {
        val state = currentState
        val viewerId = state.viewerId ?: return
        val edit = state.commentEdit?.takeIf { it.commentId == commentId } ?: return
        if (state.isChangingComments || state.detail?.canWriteComments != true || !ReviewCommentBody.isValid(edit.draft)) return
        if (managedComment(commentId) == null) {
            cancelCommentEdit()
            return
        }
        val requestEpoch = epoch
        cancelReads()
        launchResultTask(
            taskKey = SUBMIT_TASK,
            loadKey = ReviewDetailLoadKey.Submit,
            policy = TaskPolicy.IgnoreNew,
            request = { comments.editComment(reviewId, commentId, edit.draft, viewerId) },
            onSuccess = { receipt ->
                if (isCurrent(requestEpoch)) {
                    reduce {
                        copy(
                            commentEdit = null,
                            threads = threads.map { it.withEditedComment(receipt.comment) },
                        ).withCount(receipt.commentCount)
                    }
                }
            },
            onError = {
                if (isCurrent(requestEpoch)) trySideEffect(ReviewDetailEffect.ShowError(Res.string.review_comment_edit_failed))
            },
        )
    }

    private fun deleteComment(commentId: String) {
        val viewerId = currentState.viewerId ?: return
        if (currentState.isChangingComments) return
        val comment = managedComment(commentId) ?: return
        val requestEpoch = epoch
        cancelReads()
        launchResultTask(
            taskKey = COMMENT_MUTATION_TASK,
            loadKey = ReviewDetailLoadKey.CommentMutation,
            policy = TaskPolicy.IgnoreNew,
            request = { comments.deleteComment(reviewId, commentId, viewerId) },
            onSuccess = { deletion ->
                if (isCurrent(requestEpoch)) {
                    if (currentState.replyTarget?.id == commentId) requestId = null
                    reduce {
                        copy(
                            threads = threads.mapNotNull { it.afterCommentDeletion(comment, deletion) },
                            replyTarget = replyTarget?.takeUnless { it.id == commentId },
                            commentEdit = commentEdit?.takeUnless { it.commentId == commentId },
                        ).withCount(deletion.commentCount)
                    }
                    trySideEffect(ReviewDetailEffect.CommentDeleted)
                }
            },
            onError = {
                if (isCurrent(requestEpoch)) trySideEffect(ReviewDetailEffect.ShowError(Res.string.review_comment_delete_failed))
            },
        )
    }

    private fun managedComment(commentId: String): ReviewComment? =
        currentState.threads
            .asSequence()
            .flatMap { sequenceOf(it.comment) + it.replies.asSequence() }
            .firstOrNull { it.id == commentId && it.canManage && !it.isDeleted }

    private fun ReviewCommentThread.withEditedComment(updated: ReviewComment): ReviewCommentThread =
        if (comment.id == updated.id) {
            copy(comment = updated)
        } else if (comment.id == updated.parentId) {
            copy(replies = replies.map { if (it.id == updated.id) updated else it })
        } else {
            this
        }

    private fun ReviewCommentThread.afterCommentDeletion(
        removed: ReviewComment,
        deletion: ReviewCommentDeletion,
    ): ReviewCommentThread? {
        if (comment.id != (removed.parentId ?: removed.id)) return this
        if (!deletion.keepThread) return null
        if (removed.parentId != null) return copy(replies = replies.filterNot { it.id == removed.id })
        return copy(
            comment = comment.copy(body = "", nickname = "", canReport = false, canManage = false, isEdited = false, isDeleted = true),
        )
    }

    private fun applyJournal(journal: ReviewChangeJournal) {
        if (journal.epoch != epoch) {
            val previousState = currentState
            val keepGuestContent = previousState.viewerId == null && journal.userId != null && previousState.detail != null
            epoch = journal.epoch
            revision = journal.revision
            requestId = null
            cancelReads()
            listOf(DETAIL_TASK, SUBMIT_TASK, COMMENT_MUTATION_TASK, LIKE_TASK, DELETE_TASK, REPORT_TASK, NOTIFICATIONS_TASK).forEach(::cancelTask)
            reduce {
                ReviewDetailUiState(
                    viewerId = journal.userId,
                    detail = previousState.detail.takeIf { keepGuestContent },
                    threads = if (keepGuestContent) previousState.threads else emptyList(),
                    commentsLoaded = keepGuestContent && previousState.commentsLoaded,
                )
            }
            reload(keepVisibleContent = keepGuestContent)
            return
        }
        if (journal.revision <= revision) return
        val pending = journal.changesAfter(revision)
        val hasGap = journal.hasGapAfter(revision)
        revision = journal.revision
        if (hasGap || pending.any { it.change.affectsAccess() }) {
            cancelTask(SUBMIT_TASK)
            cancelTask(COMMENT_MUTATION_TASK)
            reload()
            return
        }
        for ((_, change) in pending) {
            when (change) {
                is ReviewChange.Commented ->
                    if (change.reviewId == reviewId) {
                        if (!showDetail) fetchComments(append = false)
                        reduce { withCount(change.count) }
                    }
                is ReviewChange.Liked ->
                    if (change.reviewId == reviewId) {
                        reduce { copy(detail = detail?.let { it.copy(review = it.review.copy(likeCount = change.result.likeCount, isLiked = change.result.isLiked)) }) }
                    }
                else -> Unit
            }
        }
    }

    private fun ReviewChange.affectsAccess(): Boolean = this is ReviewChange.AccessChanged || (this is ReviewChange.Written && this.reviewId == this@ReviewDetailViewModel.reviewId)

    private fun toggleLike() {
        val detail = currentState.detail?.takeIf { it.canLike } ?: return
        if (currentState.viewerId == null) {
            trySideEffect(ReviewDetailEffect.RequestLogin(Res.string.login_required_review_like_description))
            return
        }
        val requestEpoch = epoch
        launchResultTask(
            taskKey = LIKE_TASK,
            loadKey = ReviewDetailLoadKey.Like,
            policy = TaskPolicy.IgnoreNew,
            request = { reviews.setReviewLike(reviewId, !detail.review.isLiked) },
            onError = {
                if (isCurrent(requestEpoch)) trySideEffect(ReviewDetailEffect.ShowError(Res.string.review_like_failed))
            },
        )
    }

    private fun deleteReview() {
        if (currentState.detail
                ?.review
                ?.author
                ?.userId
                ?.takeIf(String::isNotBlank) != currentState.viewerId ||
            currentState.viewerId == null
        ) {
            return
        }
        val requestEpoch = epoch
        launchResultTask(
            taskKey = DELETE_TASK,
            loadKey = ReviewDetailLoadKey.Delete,
            policy = TaskPolicy.IgnoreNew,
            request = { reviews.deleteReview(reviewId) },
            onSuccess = { if (isCurrent(requestEpoch)) trySideEffect(ReviewDetailEffect.Deleted) },
            onError = { if (isCurrent(requestEpoch)) trySideEffect(ReviewDetailEffect.ShowError(Res.string.review_delete_failed)) },
        )
    }

    private fun toggleNotifications() {
        val detail = currentState.detail ?: return
        val enabled = detail.reviewNotificationsEnabled ?: return
        if (currentState.viewerId == null || currentState.viewerId != detail.review.author.userId) return
        val requestEpoch = epoch
        launchResultTask(
            taskKey = NOTIFICATIONS_TASK,
            loadKey = ReviewDetailLoadKey.Notifications,
            policy = TaskPolicy.IgnoreNew,
            request = { notifications.updateReviewEnabled(reviewId, !enabled) },
            onSuccess = { updated ->
                if (isCurrent(requestEpoch)) {
                    reduce { copy(detail = this.detail?.copy(reviewNotificationsEnabled = updated)) }
                }
            },
            onError = {
                if (isCurrent(requestEpoch)) trySideEffect(ReviewDetailEffect.ShowError(Res.string.personalization_update_failure_message))
            },
        )
    }

    private fun report(intent: ReviewDetailIntent.Report) {
        val review = currentState.detail?.review ?: return
        val commentId = intent.commentId
        val viewerId = currentState.viewerId
        if (viewerId == null) {
            val description =
                if (commentId == null) Res.string.login_required_review_report_description else Res.string.review_comment_report_login_required
            trySideEffect(ReviewDetailEffect.RequestLogin(description))
            return
        }
        if (commentId == null) {
            if (!review.allowsReportFrom(viewerId)) return
        } else if (currentState.threads.none { thread ->
                (thread.comment.id == commentId && thread.comment.canReport) || thread.replies.any { it.id == commentId && it.canReport }
            }
        ) {
            return
        }
        val requestEpoch = epoch
        launchResultTask(
            taskKey = REPORT_TASK,
            loadKey = ReviewDetailLoadKey.Report,
            policy = TaskPolicy.IgnoreNew,
            request = {
                if (commentId == null) {
                    moderation.reportReview(reviewId, intent.reason, intent.details)
                } else {
                    comments.reportComment(commentId, intent.reason, intent.details, viewerId)
                }
            },
            onSuccess = { if (isCurrent(requestEpoch)) trySideEffect(ReviewDetailEffect.Reported) },
            onError = { if (isCurrent(requestEpoch)) trySideEffect(ReviewDetailEffect.ShowError(Res.string.review_report_failed)) },
        )
    }

    private fun cancelReads() {
        cancelTask(COMMENTS_TASK)
        currentState.threads.forEach { cancelTask(replyTask(it.comment.id)) }
    }

    private fun markUnavailable() {
        cancelReads()
        reduce { copy(detail = null, threads = emptyList(), nextCursor = null, unavailable = true, replyTarget = null, commentEdit = null) }
    }

    private fun isCurrent(requestEpoch: Long): Boolean = requestEpoch == changes.journal.value.epoch && currentState.viewerId == login.currentAccountId()

    private fun ReviewDetailUiState.withCount(count: Int): ReviewDetailUiState = copy(detail = detail?.let { it.copy(review = it.review.copy(commentCount = count)) })

    private fun ReviewDetailUiState.insertSubmittedComment(comment: ReviewComment): List<ReviewCommentThread> {
        if (comment.parentId == null) {
            return (listOf(ReviewCommentThread(comment, emptyList(), null)) + threads).distinctBy { it.comment.id }
        }
        return threads.map { thread ->
            if (thread.comment.id == comment.parentId) {
                thread.copy(replies = mergeReplies(thread.replies, listOf(comment)))
            } else {
                thread
            }
        }
    }

    private fun mergeReplies(
        existing: List<ReviewComment>,
        additional: List<ReviewComment>,
    ): List<ReviewComment> = (existing + additional).distinctBy { it.id }.sortedWith(compareBy({ it.createdAt }, { it.id }))

    private fun RamapError.deniesAccess(): Boolean = this is RamapError.Http && status in ACCESS_DENIED_STATUSES

    private fun replyTask(parentId: String): String = "review-comment-replies-$parentId"

    private companion object {
        val ACCESS_DENIED_STATUSES = setOf(401, 403, 404)
        const val DETAIL_TASK = "review-detail"
        const val COMMENTS_TASK = "review-comments"
        const val SUBMIT_TASK = "review-comment-submit"
        const val COMMENT_MUTATION_TASK = "review-comment-mutation"
        const val LIKE_TASK = "review-detail-like"
        const val DELETE_TASK = "review-detail-delete"
        const val REPORT_TASK = "review-detail-report"
        const val NOTIFICATIONS_TASK = "review-detail-notifications"
    }
}
