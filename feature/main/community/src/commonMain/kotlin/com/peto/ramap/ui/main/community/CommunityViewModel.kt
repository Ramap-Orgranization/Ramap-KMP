package com.peto.ramap.ui.main.community

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.community.CommunityFeedCursor
import com.peto.ramap.domain.model.community.CommunityFeedPage
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewFeedRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.repository.UserModerationRepository
import com.peto.ramap.domain.store.ReviewChangeStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.community.contract.CommunityIntent
import com.peto.ramap.ui.main.community.contract.CommunityLoadKey
import com.peto.ramap.ui.main.community.contract.CommunitySideEffect
import com.peto.ramap.ui.main.community.contract.CommunityUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.login_required_review_like_description
import ramap.shared.generated.resources.login_required_review_report_description
import ramap.shared.generated.resources.review_action_failed
import ramap.shared.generated.resources.review_action_success
import ramap.shared.generated.resources.review_delete_failed
import ramap.shared.generated.resources.review_like_failed
import ramap.shared.generated.resources.review_report_failed
import ramap.shared.generated.resources.review_unblock_failed
import ramap.shared.generated.resources.review_unblock_success

class CommunityViewModel(
    private val reviewFeedRepository: ReviewFeedRepository,
    private val userModerationRepository: UserModerationRepository,
    private val profilesRepository: ProfileRepository,
    private val reviews: ReviewRepository,
    private val changes: ReviewChangeStore,
    private val reducer: ReviewChangeReducer,
) : BaseViewModel<CommunityUiState, CommunityIntent, CommunitySideEffect>(CommunityUiState()) {
    private var sessionEpoch: Long? = null
    private var lastAppliedRevision = changes.journal.value.revision

    init {
        observeSessionAndChanges()
    }

    private fun observeSessionAndChanges() {
        viewModelScope.launch {
            combine(profilesRepository.sessionUserId, changes.journal) { viewer, journal -> viewer to journal }
                .collect { (viewer, journal) ->
                    if (viewer != journal.userId) {
                        invalidateSession(viewer)
                        return@collect
                    }
                    if (!currentState.sessionResolved || sessionEpoch != journal.epoch || currentState.currentUserId != viewer) {
                        invalidateSession(viewer)
                        sessionEpoch = journal.epoch
                        lastAppliedRevision = journal.revision
                        reduce { copy(sessionResolved = true) }
                        loadPage(append = false)
                        return@collect
                    }
                    if (journal.hasGapAfter(lastAppliedRevision)) {
                        lastAppliedRevision = journal.revision
                        invalidateAccess()
                        reduce { copy(reviews = emptyList(), nextCursor = null, hasLoaded = false) }
                        loadPage(append = false)
                        return@collect
                    }
                    var requiresReload = false
                    for (record in journal.changesAfter(lastAppliedRevision)) {
                        val change = record.change
                        applyChange(change)
                        requiresReload = requiresReload || change is ReviewChange.AccessChanged || change is ReviewChange.Written
                        lastAppliedRevision = record.revision
                    }
                    if (requiresReload) loadPage(append = false)
                }
        }
    }

    private fun invalidateSession(viewer: String?) {
        cancelTasks()
        sessionEpoch = null
        reduce { CommunityUiState(currentUserId = viewer) }
    }

    private fun invalidateAccess() {
        cancelTasks()
        reduce { clearAccess() }
    }

    private fun cancelTasks() {
        for (key in listOf(PAGE_TASK, ACTION_TASK, REVEAL_TASK, REPORT_TASK, UNBLOCK_TASK)) cancelTask(key)
    }

    private fun applyChange(change: ReviewChange) {
        if (change is ReviewChange.AccessChanged) invalidateAccess()
        reduce {
            val knownIds = reviews.map { it.review.id }.toSet()
            val patched =
                reducer.updateProfileRows(
                    rows = reviews,
                    change = change,
                    hasMore = nextCursor != null,
                    accepts = { receipt, review ->
                        // Receipts contain owner-only rows; new eligibility is decided by the feed RPC.
                        review.id in knownIds && reducer.isPublicPublished(review) && receipt.counts.profileIsPublic
                    },
                )
            copy(reviews = patched, revealed = revealed.filterKeys { id -> patched.any { it.review.id == id && it.review.isBlocked } })
        }
    }

    override suspend fun handleIntent(intent: CommunityIntent) {
        when (intent) {
            CommunityIntent.Refresh -> {
                invalidateAccess()
                loadPage(append = false)
            }
            CommunityIntent.Retry -> loadPage(append = currentState.appendFailed)
            CommunityIntent.LoadMore -> {
                val state = currentState
                if (state.nextCursor != null && !state.refreshing && !state.appending && !state.appendFailed) loadPage(append = true)
            }
            is CommunityIntent.ToggleLike -> toggleLike(intent.review)
            is CommunityIntent.Reveal -> reveal(intent.review)
            is CommunityIntent.RequestUnblock -> {
                val review = findBlocked(intent.review)
                if (currentState.currentUserId != null && !currentState.unblocking) reduce { copy(unblockReview = review) }
            }
            CommunityIntent.ConfirmUnblock -> unblock()
            CommunityIntent.DismissUnblock -> if (!currentState.unblocking) reduce { copy(unblockReview = null) }
            is CommunityIntent.RequestReport -> requestReport(intent.review)
            is CommunityIntent.SubmitReport -> submitReport(intent.reason, intent.details)
            CommunityIntent.DismissReport -> if (!currentState.reporting) reduce { copy(reportReview = null) }
            is CommunityIntent.RequestDelete -> {
                val review = findReview(intent.review)
                if (review?.allowsDeletionBy(currentState.currentUserId) == true) reduce { copy(deleteReview = review) }
            }
            CommunityIntent.ConfirmDelete -> delete()
            CommunityIntent.DismissDelete -> if (currentState.actingId == null) reduce { copy(deleteReview = null) }
        }
    }

    private fun loadPage(append: Boolean) {
        if (!currentState.sessionResolved) return
        val cursor = if (append) currentState.nextCursor ?: return else null
        val extent = if (append) 0 else currentState.reviews.size
        val baseline = changes.journal.value
        launchResultTask(
            taskKey = PAGE_TASK,
            loadKey = if (append) CommunityLoadKey.Append else CommunityLoadKey.Page,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(failed = false, appendFailed = false) },
            request = { fetchExtent(cursor, extent) },
            onSuccess = { page ->
                if (baseline.epoch != changes.journal.value.epoch) return@launchResultTask
                val journal = changes.journal.value
                val records = journal.changesAfter(baseline.revision)
                if (journal.hasGapAfter(baseline.revision) || records.any { it.change is ReviewChange.Written || it.change is ReviewChange.AccessChanged }) {
                    loadPage(append = false)
                    return@launchResultTask
                }
                var rows = page.reviews
                val known = currentState.reviews.associateBy { it.review.id }
                rows = rows.map { it.copy(review = reducer.retainMedia(it.review, known[it.review.id]?.review)) }
                for (record in records) {
                    rows = reducer.updateProfileRows(rows, record.change, page.nextCursor != null) { _, _ -> false }
                }
                reduce {
                    copy(
                        reviews = (if (append) reviews + rows else rows).distinctBy { it.review.id },
                        nextCursor = page.nextCursor,
                        hasLoaded = true,
                    )
                }
            },
            onError = {
                if (baseline.epoch == changes.journal.value.epoch) {
                    reduce { copy(failed = !append, appendFailed = append) }
                }
            },
        )
    }

    private suspend fun fetchExtent(
        cursor: CommunityFeedCursor?,
        extent: Int,
    ): RamapResult<CommunityFeedPage> {
        var next = cursor
        val rows = mutableListOf<ProfileReview>()
        do {
            val previous = next
            when (val result = reviewFeedRepository.fetchPage(next)) {
                is RamapResult.Error -> return result
                is RamapResult.Success -> {
                    rows += result.data.reviews
                    next = result.data.nextCursor
                }
            }
            currentCoroutineContext().ensureActive()
            if (next == previous) next = null
        } while (rows.size < extent && next != null)
        return RamapResult.Success(CommunityFeedPage(rows, next))
    }

    private fun findReview(review: Review): Review? = currentState.reviews.firstOrNull { it.review.id == review.id }?.review

    private fun findBlocked(review: Review): Review? = findReview(review)?.takeIf { it.isBlockedVersionOf(review) }

    private fun reveal(review: Review) {
        val blocked = findBlocked(review) ?: return
        if (currentState.currentUserId == null || currentState.refreshing || currentState.unblocking || blocked.id in currentState.revealed) return
        val epoch = sessionEpoch
        launchResultTask(
            taskKey = REVEAL_TASK,
            loadKey = CommunityLoadKey.Reveal,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(revealingId = blocked.id) },
            onFinish = { copy(revealingId = null) },
            request = { reviewFeedRepository.fetchBlockedReviewOnce(blocked.id) },
            onSuccess = { revealed ->
                if (epoch != sessionEpoch || findBlocked(blocked) == null) return@launchResultTask
                if (revealed == null || !revealed.isBlockedVersionOf(blocked)) {
                    showToast(Res.string.review_action_failed)
                } else {
                    reduce { copy(revealed = this.revealed + (revealed.id to revealed)) }
                }
            },
            onError = { showToast(Res.string.review_action_failed) },
        )
    }

    private fun toggleLike(review: Review) {
        val row = findReview(review) ?: return
        if (row.isBlocked || !row.allowsLikeFrom(currentState.currentUserId)) return
        if (currentState.currentUserId == null) {
            trySideEffect(CommunitySideEffect.LoginRequired(Res.string.login_required_review_like_description))
            return
        }
        val epoch = sessionEpoch
        val next = row.toggledLike()
        launchResultTask(
            taskKey = ACTION_TASK,
            loadKey = CommunityLoadKey.Action,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(actingId = row.id, reviews = patchLike(reviews, row.id, next)) },
            onFinish = { copy(actingId = null) },
            request = { reviews.setReviewLike(row.id, next.isLiked) },
            onSuccess = { like ->
                if (epoch == sessionEpoch) reduce { copy(reviews = patchLike(reviews, row.id, like)) }
            },
            onError = {
                if (epoch == sessionEpoch) {
                    reduce { copy(reviews = patchLike(reviews, row.id, ReviewLike(row.likeCount, row.isLiked))) }
                    showToast(Res.string.review_like_failed)
                }
            },
        )
    }

    private fun patchLike(
        rows: List<ProfileReview>,
        id: String,
        like: ReviewLike,
    ): List<ProfileReview> = reducer.updateProfileRows(rows, ReviewChange.Liked(id, like), currentState.nextCursor != null) { _, _ -> false }

    private fun requestReport(review: Review) {
        val row = findReview(review) ?: return
        if (currentState.currentUserId == null) {
            trySideEffect(CommunitySideEffect.LoginRequired(Res.string.login_required_review_report_description))
            return
        }
        if (!row.allowsReportFrom(currentState.currentUserId) || currentState.reporting) return
        val displayed = if (row.isBlocked) currentState.revealed[row.id] ?: return else row
        reduce { copy(reportReview = displayed) }
    }

    private fun submitReport(
        reason: ReportReason,
        details: String,
    ) {
        val target = currentState.reportReview ?: return
        if (!target.allowsReportFrom(currentState.currentUserId) || !reason.isValidDetails(details)) return
        launchResultTask(
            taskKey = REPORT_TASK,
            loadKey = CommunityLoadKey.Report,
            policy = TaskPolicy.IgnoreNew,
            request = { userModerationRepository.reportReview(target.id, reason, details) },
            onSuccess = {
                if (currentState.reportReview?.id == target.id) {
                    reduce { copy(reportReview = null) }
                    showToast(Res.string.review_action_success, ToastType.SUCCESS)
                }
            },
            onError = { showToast(Res.string.review_report_failed) },
        )
    }

    private fun unblock() {
        val target = currentState.unblockReview ?: return
        if (currentState.currentUserId == null || findBlocked(target) == null) return
        launchResultTask(
            taskKey = UNBLOCK_TASK,
            loadKey = CommunityLoadKey.Unblock,
            policy = TaskPolicy.IgnoreNew,
            request = { userModerationRepository.unblockUser(target.author.userId) },
            onSuccess = {
                reduce { clearAccess() }
                showToast(Res.string.review_unblock_success, ToastType.SUCCESS)
            },
            onError = { showToast(Res.string.review_unblock_failed) },
        )
    }

    private fun delete() {
        val target = currentState.deleteReview ?: return
        if (!target.allowsDeletionBy(currentState.currentUserId) || findReview(target) == null) return
        launchResultTask(
            taskKey = ACTION_TASK,
            loadKey = CommunityLoadKey.Action,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(actingId = target.id) },
            onFinish = { copy(actingId = null) },
            request = { reviews.deleteReview(target.id) },
            onSuccess = {
                reduce { copy(deleteReview = null) }
            },
            onError = { showToast(Res.string.review_delete_failed) },
        )
    }

    private fun showToast(
        message: StringResource,
        type: ToastType = ToastType.ERROR,
    ) {
        trySideEffect(CommunitySideEffect.ShowToast(ToastData(message = message, type = type)))
    }

    private companion object {
        const val PAGE_TASK = "community_page"
        const val ACTION_TASK = "community_action"
        const val REVEAL_TASK = "community_reveal"
        const val REPORT_TASK = "community_report"
        const val UNBLOCK_TASK = "community_unblock"
    }
}
