package com.peto.ramap.ui.hidden.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface HiddenShopListSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
    ) : HiddenShopListSideEffect
}
