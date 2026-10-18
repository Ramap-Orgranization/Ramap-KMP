package com.peto.ramap.ui.shop.review.contract

import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.ui.base.SideEffect
import org.jetbrains.compose.resources.StringResource

sealed interface ReviewsSideEffect : SideEffect {
    data object ShowLoginGuide : ReviewsSideEffect

    data class ShowToast(
        val message: StringResource,
        val type: ToastType = ToastType.DEFAULT,
    ) : ReviewsSideEffect
}
