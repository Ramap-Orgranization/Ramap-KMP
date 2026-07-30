package com.peto.ramap.ui.bookmark.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface BookmarkedShopListSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
    ) : BookmarkedShopListSideEffect
}
