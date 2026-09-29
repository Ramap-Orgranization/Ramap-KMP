package com.peto.ramap.ui.review.write.contract

import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class ReviewWriteUiState(
    val body: String = "",
    val images: List<ReviewImage> = emptyList(),
    val existingImages: List<ExistingReviewPhoto> = emptyList(),
    val reviewId: String? = null,
    val reviewLoadFailed: Boolean = false,
    val reviewLoaded: Boolean = false,
    val isPublic: Boolean = true,
    val shop: RamenShop? = null,
    val shopLoadFailed: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<ReviewWriteUiState> {
    val isLoadingShop: Boolean get() = loadState.isLoading(ReviewWriteLoadKey.Shop)
    val isLoadingReview: Boolean get() = loadState.isLoading(ReviewWriteLoadKey.Review)
    val isSubmitting: Boolean get() = loadState.isLoading(ReviewWriteLoadKey.Submit)
    val canEdit: Boolean
        get() = !isSubmitting && (reviewId == null || (reviewLoaded && !isLoadingReview && !reviewLoadFailed))
    val canSubmit: Boolean
        get() = Review.isValidBody(body) && canEdit

    override fun withLoadingState(loadState: LoadState): ReviewWriteUiState = copy(loadState = loadState)
}
