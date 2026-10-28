package com.peto.ramap.ui.main.map.archive

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.review.ArchiveShops
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeJournal
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.ArchiveRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.store.ReviewChangeStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapIntent
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapLoadKey
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapSideEffect
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapUiState
import com.peto.ramap.ui.main.map.archive.model.ArchiveMapSnapshot
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_delete_failed
import kotlin.time.Clock

class ArchiveMapViewModel(
    private val repository: ArchiveRepository,
    private val changes: ReviewChangeStore,
    private val reducer: ReviewChangeReducer,
    private val reviews: ReviewRepository,
) : BaseViewModel<ArchiveMapUiState, ArchiveMapIntent, ArchiveMapSideEffect>(
        ArchiveMapUiState(userId = changes.journal.value.userId, reviewRevision = changes.journal.value.revision),
    ) {
    private val markerPhotoJobs = mutableMapOf<Set<String>, Job>()

    init {
        viewModelScope.launch {
            changes.journal.collectLatest { journal ->
                if (journal.userId != currentState.userId) {
                    reset(journal.userId, journal.revision)
                    if (currentState.isEntered && journal.userId != null) loadVisits()
                    return@collectLatest
                }
                if (!currentState.hasLoaded) return@collectLatest
                if (journal.hasGapAfter(currentState.reviewRevision)) {
                    loadVisits()
                    return@collectLatest
                }
                val pendingChanges = journal.changesAfter(currentState.reviewRevision)
                if (pendingChanges.any { (it.change as? ReviewChange.AccessChanged)?.userId == journal.userId }) {
                    loadVisits()
                    return@collectLatest
                }
                applyJournal(journal)
            }
        }
    }

    override suspend fun handleIntent(intent: ArchiveMapIntent) {
        when (intent) {
            ArchiveMapIntent.Exit -> {
                reduce { copy(isEntered = false) }
                selectShop(null)
            }
            ArchiveMapIntent.Enter -> {
                reduce { copy(isEntered = true) }
                if (!currentState.hasLoaded && !currentState.hasError && !currentState.isLoading) loadVisits() else loadMarkerPhotos()
            }
            ArchiveMapIntent.Retry -> {
                cancelMarkerPhotos()
                reduce { copy(photoState = photoState.clearRefreshes()) }
                loadVisits()
            }
            is ArchiveMapIntent.RequestReviewDeletion -> {
                if (!currentState.isDeletingReview && currentState.selectedReviews.any { it.review.id == intent.reviewId }) {
                    reduce { copy(deleteTargetReviewId = intent.reviewId) }
                }
            }
            ArchiveMapIntent.DismissReviewDeletion -> {
                if (!currentState.isDeletingReview) reduce { copy(deleteTargetReviewId = null) }
            }
            ArchiveMapIntent.ConfirmReviewDeletion -> deleteReview()
            ArchiveMapIntent.FocusConsumed -> {
                reduce { copy(hasFocusedVisits = true) }
            }
            is ArchiveMapIntent.CameraChanged -> reduce { copy(cameraPosition = intent.position) }
            is ArchiveMapIntent.SelectShop -> selectShop(intent.shopId)
            ArchiveMapIntent.RetryReviewPhotos -> loadReviewPhotos(retry = true)
            is ArchiveMapIntent.ReviewPhotoDownloadFailed -> reviewPhotoDownloadFailed(intent.reference)
            is ArchiveMapIntent.ReviewPhotoDownloadSucceeded -> reviewPhotoDownloadSucceeded(intent.reference)
            is ArchiveMapIntent.PhotoDownloadFailed -> markerPhotoDownloadFailed(intent.shopId, intent.reference)
        }
    }

    private fun deleteReview() {
        val reviewId = currentState.deleteTargetReviewId ?: return
        if (currentState.selectedReviews.none { it.review.id == reviewId }) return
        val userId = currentState.userId?.takeIf(::isCurrentUser) ?: return
        launchResultTask(
            taskKey = DELETE_REVIEW_TASK,
            loadKey = ArchiveMapLoadKey.DeleteReview,
            policy = TaskPolicy.IgnoreNew,
            request = { reviews.deleteReview(reviewId) },
            onSuccess = {
                if (isCurrentUser(userId) && currentState.deleteTargetReviewId == reviewId) {
                    reduce { copy(deleteTargetReviewId = null) }
                }
            },
            onError = {
                if (isCurrentUser(userId)) {
                    postSideEffect(ArchiveMapSideEffect.ShowToast(ToastData(Res.string.review_delete_failed, ToastType.ERROR)))
                }
            },
        )
    }

    private fun reset(
        userId: String?,
        nextRevision: Long,
    ) {
        cancelTask(DELETE_REVIEW_TASK)
        cancelTask(REVIEW_PHOTOS_TASK)
        cancelTask(VISITS_TASK)
        cancelTask("visited-shops")
        cancelMarkerPhotos()
        reduce { ArchiveMapUiState(userId = userId, reviewRevision = nextRevision, isEntered = isEntered) }
    }

    private fun loadVisits() {
        val userId = currentState.userId?.takeIf(::isCurrentUser) ?: return
        launchResultTask(
            taskKey = VISITS_TASK,
            loadKey = ArchiveMapLoadKey.Visits,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(hasError = false) },
            request = {
                // A write can shift offset pages. Restart the complete snapshot until its revision is stable.
                var baseline = changes.journal.value
                var result = repository.fetchArchiveShops()
                currentCoroutineContext().ensureActive()
                while (isCurrentUser(userId) && baseline.revision != changes.journal.value.revision && result is RamapResult.Success) {
                    baseline = changes.journal.value
                    result = repository.fetchArchiveShops()
                    currentCoroutineContext().ensureActive()
                }
                when (result) {
                    is RamapResult.Success -> RamapResult.Success(ArchiveMapSnapshot(result.data, baseline.userId, baseline.revision))
                    is RamapResult.Error -> result
                }
            },
            onSuccess = { snapshot ->
                val journal = changes.journal.value
                if (!isCurrentUser(userId) || snapshot.userId != userId) return@launchResultTask
                if (journal.hasGapAfter(snapshot.revision)) {
                    // Schedule after this task finishes so IgnoreNew does not swallow recovery.
                    dispatch(ArchiveMapIntent.Retry)
                    return@launchResultTask
                }
                applyJournal(journal, snapshot)
            },
            onError = { if (isCurrentUser(userId)) reduce { copy(hasError = true) } },
        )
    }

    private fun applyJournal(
        journal: ReviewChangeJournal,
        snapshot: ArchiveMapSnapshot? = null,
    ) {
        val previousReviews = currentState.selectedVisitReviews()
        val pendingChanges = journal.changesAfter(snapshot?.revision ?: currentState.reviewRevision)
        val firstCreatedShopId =
            if (currentState.visits.reviews.isEmpty()) {
                pendingChanges
                    .mapNotNull { it.change as? ReviewChange.Written }
                    .lastOrNull { it.userId == journal.userId && it.before == null && it.after != null }
                    ?.shopId
            } else {
                null
            }
        reduce {
            var nextVisits = snapshot?.visits ?: visits
            for (entry in pendingChanges) nextVisits = applyChange(nextVisits, entry.change, userId)
            copy(pendingShopSelection = firstCreatedShopId ?: pendingShopSelection).withVisits(nextVisits).copy(
                reviewRevision = journal.revision,
                hasLoaded = hasLoaded || snapshot != null,
                hasError = if (snapshot != null) false else hasError,
                hasFocusedVisits = if (firstCreatedShopId != null) false else hasFocusedVisits,
            )
        }
        reconcileVisits(previousReviews)
        val missing =
            currentState.visits.reviews
                .map { it.review.shopId }
                .toSet() - currentState.shops.keys
        if (missing.isNotEmpty()) loadMissingShops(missing)
    }

    private fun applyChange(
        visits: ArchiveShops,
        change: ReviewChange,
        userId: String?,
    ): ArchiveShops {
        if (change is ReviewChange.AccessChanged) return visits
        if (change is ReviewChange.Written && change.userId != userId) return visits
        val snapshot =
            when (change) {
                is ReviewChange.Written -> change.after
                is ReviewChange.Hydrated -> change.snapshot
                else -> null
            }
        val rows = reducer.updateOwnerRows(visits.reviews, change, MyReviewVisibility.ALL, hasMore = false)
        val shops = snapshot?.shop?.let { RamenShops(visits.shops + (it.id to it)) } ?: visits.shops
        return ArchiveShops(rows, shops)
    }

    private fun loadMissingShops(ids: Set<String>) {
        val userId = currentState.userId?.takeIf(::isCurrentUser) ?: return
        launchResultTask(
            taskKey = "visited-shops",
            request = { repository.fetchShops(ids) },
            onSuccess = { shops ->
                if (!isCurrentUser(userId)) return@launchResultTask
                val previousReviews = currentState.selectedVisitReviews()
                reduce { withVisits(visits.copy(shops = RamenShops(visits.shops + shops))) }
                reconcileVisits(previousReviews)
            },
            onError = { if (isCurrentUser(userId)) reduce { copy(hasError = true) } },
        )
    }

    private fun reconcileVisits(previousReviews: List<MyReview>) {
        val selectionChanged = previousReviews != currentState.selectedVisitReviews()
        if (selectionChanged) cancelTask(REVIEW_PHOTOS_TASK)
        loadMarkerPhotos()
        if (selectionChanged) loadReviewPhotos()
    }

    private fun loadMarkerPhotos() {
        val activeReferences = markerPhotoJobs.keys.flatten().toSet()
        val references =
            currentState.photos.values
                .filter { it.url == null && it.reference !in activeReferences }
                .map { it.reference }
                .toSet()
        if (references.isNotEmpty()) resolvePhotos(references)
    }

    private fun resolvePhotos(references: Set<String>) {
        val userId = currentState.userId?.takeIf(::isCurrentUser) ?: return
        val job =
            launchResultTask(
                taskKey = markerPhotoTaskKey(references),
                policy = TaskPolicy.IgnoreNew,
                request = { repository.resolvePhotos(references) },
                onSuccess = { resolved ->
                    if (!isCurrentUser(userId)) return@launchResultTask
                    val activeReferences =
                        currentState.photos.values
                            .map { it.reference }
                            .toSet()
                    reduce { copy(photoState = photoState.withMarkerPhotos(resolved, references.intersect(activeReferences))) }
                },
                onError = {
                    if (isCurrentUser(userId)) {
                        val activeReferences =
                            currentState.photos.values
                                .map { it.reference }
                                .toSet()
                        reduce { copy(photoState = photoState.withMarkerPhotos(emptyMap(), references.intersect(activeReferences))) }
                    }
                },
            )
        if (job == null) return
        markerPhotoJobs[references] = job
        job.invokeOnCompletion {
            if (markerPhotoJobs[references] === job) markerPhotoJobs.remove(references)
        }
    }

    private fun markerPhotoTaskKey(references: Set<String>): String = "visited-photos:${references.sorted().joinToString { "${it.length}:$it" }}"

    private fun cancelMarkerPhotos() {
        markerPhotoJobs.keys.toList().forEach { cancelTask(markerPhotoTaskKey(it)) }
        markerPhotoJobs.clear()
    }

    private fun isCurrentUser(userId: String): Boolean = userId == currentState.userId && userId == changes.journal.value.userId

    private fun selectShop(shopId: String?) {
        if (shopId == currentState.selectedShopId && currentState.pendingShopSelection == null) return
        cancelTask(REVIEW_PHOTOS_TASK)
        reduce {
            copy(
                selectedShopId = shopId?.takeIf { it in shops },
                pendingShopSelection = null,
                deleteTargetReviewId = null,
                photoState = photoState.clearSelection(),
            )
        }
        loadReviewPhotos()
    }

    private fun loadReviewPhotos(retry: Boolean = false) {
        val userId = currentState.userId?.takeIf(::isCurrentUser) ?: return
        val shopId = currentState.selectedShopId ?: return
        val references =
            currentState.selectedReviews
                .flatMap { it.review.images }
                .filter { it.url == null || it.reference in currentState.photoState.refreshingReviewDownloads || retry && it.reference in currentState.photoState.failedReviewDownloads }
                .map { it.reference }
                .toSet()
        if (references.isEmpty()) return
        launchResultTask(
            taskKey = REVIEW_PHOTOS_TASK,
            loadKey = ArchiveMapLoadKey.ReviewPhotos,
            onStart = { copy(photoState = photoState.copy(hasReviewSigningError = false)) },
            request = {
                markerPhotoJobs
                    .filterKeys { batch -> batch.any { it in references } }
                    .values
                    .toList()
                    .forEach { it.join() }
                val photoState = currentState.photoState
                val unresolved = references.filter { it in photoState.failedReviewDownloads || it in photoState.refreshingReviewDownloads || photoState.resolved[it]?.url == null }.toSet()
                if (unresolved.isEmpty()) RamapResult.Success(emptyMap()) else repository.resolvePhotos(unresolved)
            },
            onSuccess = { photos ->
                if (!isCurrentUser(userId) || shopId != currentState.selectedShopId) return@launchResultTask
                reduce { copy(photoState = photoState.withReviewPhotos(photos, references)) }
            },
            onError = {
                if (isCurrentUser(userId) && shopId == currentState.selectedShopId) {
                    reduce { copy(photoState = photoState.withReviewSigningFailure(references)) }
                }
            },
        )
    }

    private fun markerPhotoDownloadFailed(
        shopId: String,
        reference: String,
    ) {
        val photo = currentState.photos[shopId] ?: return
        if (photo.reference != reference || photo.expiresAt?.let { it > Clock.System.now() } != false) return
        if (reference in currentState.photoState.refreshedMarkers) return
        reduce { copy(photoState = photoState.copy(refreshedMarkers = photoState.refreshedMarkers + reference)) }
        resolvePhotos(setOf(reference))
    }

    private fun reviewPhotoDownloadFailed(reference: String) {
        val photo = currentState.selectedReviews.flatMap { it.review.images }.firstOrNull { it.reference == reference } ?: return
        if (reference in currentState.photoState.refreshingReviewDownloads) return
        val shouldRefresh = photo.expiresAt?.let { it <= Clock.System.now() } == true && reference !in currentState.photoState.refreshedReviews
        reduce {
            copy(
                photoState =
                    photoState.copy(
                        failedReviewDownloads = if (shouldRefresh) photoState.failedReviewDownloads else photoState.failedReviewDownloads + reference,
                        refreshingReviewDownloads = if (shouldRefresh) photoState.refreshingReviewDownloads + reference else photoState.refreshingReviewDownloads,
                        refreshedReviews = if (shouldRefresh) photoState.refreshedReviews + reference else photoState.refreshedReviews,
                    ),
            )
        }
        if (shouldRefresh) loadReviewPhotos()
    }

    private fun reviewPhotoDownloadSucceeded(reference: String) {
        if (currentState.selectedReviews.none { row -> row.review.images.any { it.reference == reference && it.url != null } }) return
        reduce { copy(photoState = photoState.copy(failedReviewDownloads = photoState.failedReviewDownloads - reference)) }
    }

    private companion object {
        const val DELETE_REVIEW_TASK = "visited-delete-review"
        const val REVIEW_PHOTOS_TASK = "visited-review-photos"
        const val VISITS_TASK = "visited-load"
    }
}
