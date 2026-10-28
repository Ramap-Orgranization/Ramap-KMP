package com.peto.ramap.ui.main.map.archive.contract

import com.peto.ramap.ui.base.Intent
import com.peto.ramap.ui.main.map.archive.model.CameraPosition

sealed interface ArchiveMapIntent : Intent {
    data object Enter : ArchiveMapIntent

    data object Exit : ArchiveMapIntent

    data object Retry : ArchiveMapIntent

    data object RetryReviewPhotos : ArchiveMapIntent

    data class ReviewPhotoDownloadFailed(
        val reference: String,
    ) : ArchiveMapIntent

    data class ReviewPhotoDownloadSucceeded(
        val reference: String,
    ) : ArchiveMapIntent

    data class RequestReviewDeletion(
        val reviewId: String,
    ) : ArchiveMapIntent

    data object DismissReviewDeletion : ArchiveMapIntent

    data object ConfirmReviewDeletion : ArchiveMapIntent

    data object FocusConsumed : ArchiveMapIntent

    data class CameraChanged(
        val position: CameraPosition,
    ) : ArchiveMapIntent

    data class SelectShop(
        val shopId: String?,
    ) : ArchiveMapIntent

    data class PhotoDownloadFailed(
        val shopId: String,
        val reference: String,
    ) : ArchiveMapIntent
}
