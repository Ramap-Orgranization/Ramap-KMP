package com.peto.ramap.ui.profile.review.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface ProfileReviewSideEffect : SideEffect {
    data object LoginRequired : ProfileReviewSideEffect

    data class ShowToast(
        val data: ToastData,
    ) : ProfileReviewSideEffect
}
