package com.peto.ramap.ui.main.my.contract

import com.peto.ramap.core.base.SideEffect
import com.peto.ramap.designsystem.toast.model.ToastData

sealed interface MyTabSideEffect : SideEffect {
    data object ShowMyLoginGuide : MyTabSideEffect

    data class ShowMyToast(
        val data: ToastData,
    ) : MyTabSideEffect

    data object NavigateToHiddenShops : MyTabSideEffect
}
