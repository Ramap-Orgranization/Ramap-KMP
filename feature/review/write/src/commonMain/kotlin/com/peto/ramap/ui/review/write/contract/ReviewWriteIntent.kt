package com.peto.ramap.ui.review.write.contract

import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.ui.base.Intent

sealed interface ReviewWriteIntent : Intent {
    data class Open(
        val shopId: String,
        val reviewId: String? = null,
    ) : ReviewWriteIntent

    data class ChangeBody(
        val body: String,
    ) : ReviewWriteIntent

    data class AddImage(
        val image: ReviewImage,
    ) : ReviewWriteIntent

    data class RemoveImage(
        val index: Int,
    ) : ReviewWriteIntent

    data class RemoveExistingImage(
        val index: Int,
    ) : ReviewWriteIntent

    data object ReloadReview : ReviewWriteIntent

    data class MoveImage(
        val fromIndex: Int,
        val toIndex: Int,
    ) : ReviewWriteIntent

    data class ChangeVisibility(
        val isPublic: Boolean,
    ) : ReviewWriteIntent

    data object Load : ReviewWriteIntent

    data object Submit : ReviewWriteIntent

    data object ConfirmSubmitWithPrivateProfile : ReviewWriteIntent

    data object ConfirmSubmitWithPublicProfile : ReviewWriteIntent

    data object CancelPrivateProfileConfirmation : ReviewWriteIntent
}
