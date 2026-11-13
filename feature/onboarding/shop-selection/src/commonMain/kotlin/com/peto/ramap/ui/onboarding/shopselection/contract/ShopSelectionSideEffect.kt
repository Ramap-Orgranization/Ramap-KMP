package com.peto.ramap.ui.onboarding.shopselection.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

public sealed interface ShopSelectionSideEffect : SideEffect {
    data class Completed(
        val savedShopCount: Int,
    ) : ShopSelectionSideEffect

    data class ShowToast(
        val data: ToastData,
    ) : ShopSelectionSideEffect
}
