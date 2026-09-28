package com.peto.ramap.ui.review.write.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface ReviewWriteSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
        val canRetry: Boolean = false,
    ) : ReviewWriteSideEffect

    data object Submitted : ReviewWriteSideEffect

    data object LoginRequired : ReviewWriteSideEffect
}
