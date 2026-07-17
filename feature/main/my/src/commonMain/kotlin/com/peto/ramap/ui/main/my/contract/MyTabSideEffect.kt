package com.peto.ramap.ui.main.my.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface MyTabSideEffect : SideEffect {
    data class ShowMyToast(
        val data: ToastData,
    ) : MyTabSideEffect

    data object NavigateToHiddenShops : MyTabSideEffect
}
