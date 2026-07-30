package com.peto.ramap.ui.subscribed.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface SubscribedShopListSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
    ) : SubscribedShopListSideEffect
}
