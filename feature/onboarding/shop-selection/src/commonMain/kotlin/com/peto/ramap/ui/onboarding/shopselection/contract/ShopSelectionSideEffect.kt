package com.peto.ramap.ui.onboarding.shopselection.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

public sealed interface ShopSelectionSideEffect : SideEffect {
    data object Completed : ShopSelectionSideEffect

    data class ShowToast(
        val data: ToastData,
    ) : ShopSelectionSideEffect
}
