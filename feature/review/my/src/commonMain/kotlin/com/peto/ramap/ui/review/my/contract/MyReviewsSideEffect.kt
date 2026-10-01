package com.peto.ramap.ui.review.my.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface MyReviewsSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
    ) : MyReviewsSideEffect
}
