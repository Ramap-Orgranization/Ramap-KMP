package com.peto.ramap.ui.review.other.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface OtherReviewsEffect : SideEffect {
    data object LoginRequired : OtherReviewsEffect

    data class ShowToast(
        val data: ToastData,
    ) : OtherReviewsEffect
}
