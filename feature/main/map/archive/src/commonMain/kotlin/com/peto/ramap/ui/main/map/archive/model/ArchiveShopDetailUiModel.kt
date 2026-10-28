package com.peto.ramap.ui.main.map.archive.model

import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.shop.RamenShop

data class ArchiveShopDetailUiModel(
    val shop: RamenShop,
    val reviews: List<MyReview>,
    val isReviewPhotosLoading: Boolean,
    val hasReviewPhotoError: Boolean,
    val reviewPhotoRetryKeys: Map<String, Int>,
    val onRetryReviewPhotos: () -> Unit,
    val onReviewPhotoDownloadFailed: (String) -> Unit,
    val onReviewPhotoDownloadSucceeded: (String) -> Unit,
    val deleteTargetReviewId: String?,
    val isDeletingReview: Boolean,
    val onRequestReviewDeletion: (String) -> Unit,
    val onDismissReviewDeletion: () -> Unit,
    val onConfirmReviewDeletion: () -> Unit,
    val onDismiss: () -> Unit,
)
