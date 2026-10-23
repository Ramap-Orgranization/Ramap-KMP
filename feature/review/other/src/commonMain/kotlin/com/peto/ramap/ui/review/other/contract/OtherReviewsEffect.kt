package com.peto.ramap.ui.review.other.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect
import org.jetbrains.compose.resources.StringResource

sealed interface OtherReviewsEffect : SideEffect {
    data class LoginRequired(
        val description: StringResource,
    ) : OtherReviewsEffect

    data class ShowToast(
        val data: ToastData,
    ) : OtherReviewsEffect
}
