package com.peto.ramap.ui.main.map.archive.contract

import com.peto.ramap.domain.model.review.ArchiveShops
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState
import com.peto.ramap.ui.main.map.archive.model.CameraPosition

data class ArchiveMapUiState(
    val userId: String? = null,
    val visits: ArchiveShops = ArchiveShops(emptyList(), RamenShops(emptyMap())),
    val photoState: ArchiveMapPhotoState = ArchiveMapPhotoState(),
    val reviewRevision: Long = 0,
    val isEntered: Boolean = false,
    val hasFocusedVisits: Boolean = false,
    val selectedShopId: String? = null,
    val pendingShopSelection: String? = null,
    val deleteTargetReviewId: String? = null,
    val cameraPosition: CameraPosition? = null,
    val hasLoaded: Boolean = false,
    val hasError: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<ArchiveMapUiState> {
    val shops: RamenShops get() = visits.shops

    val photos: Map<String, MyReviewPhoto> = visits.representativePhotos().mapValues { (_, photo) -> photoState.resolve(photo) }

    val selectedReviews: List<MyReview> = selectedVisitReviews().map(photoState::resolve)

    val reviewPhotoRetryKeys: Map<String, Int> get() = photoState.reviewRetryKeys

    val hasReviewPhotoError: Boolean =
        selectedReviews.any { row ->
            row.review.images.any { it.reference in photoState.failedReviewDownloads || photoState.hasReviewSigningError && it.url == null }
        }

    val shouldFocusVisits: Boolean get() = !hasFocusedVisits && shops.isNotEmpty()

    val isLoading: Boolean get() = loadState.isLoading(ArchiveMapLoadKey.Visits)

    val isReviewPhotosLoading: Boolean get() = loadState.isLoading(ArchiveMapLoadKey.ReviewPhotos)

    val isDeletingReview: Boolean get() = loadState.isLoading(ArchiveMapLoadKey.DeleteReview)

    fun selectedVisitReviews(): List<MyReview> = selectedShopId?.let(visits::reviewsForShop).orEmpty()

    fun withVisits(visits: ArchiveShops): ArchiveMapUiState {
        val ids = visits.reviews.map { it.review.shopId }.toSet()
        val nextVisits = visits.copy(shops = visits.shops.filterByShopIds(ids))
        val pendingId = pendingShopSelection?.takeIf { it in ids }
        val selectedId = (pendingId ?: selectedShopId)?.takeIf { it in nextVisits.shops }
        val selected = selectedId?.let(nextVisits::reviewsForShop).orEmpty()
        val references = selected.flatMap { it.review.images }.map { it.reference }.toSet()
        return copy(
            visits = nextVisits,
            selectedShopId = selectedId,
            pendingShopSelection = pendingId?.takeUnless { it == selectedId },
            deleteTargetReviewId = deleteTargetReviewId?.takeIf { id -> selected.any { it.review.id == id } },
            photoState = photoState.retainReviews(references),
        )
    }

    override fun withLoadingState(loadState: LoadState): ArchiveMapUiState = copy(loadState = loadState)
}
