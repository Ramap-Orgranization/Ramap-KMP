package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.ui.base.SideEffect
import com.peto.ramap.designsystem.toast.model.ToastData

sealed interface MapSideEffect : SideEffect {
    data object ShowLoginGuide : MapSideEffect

    data class ShowToast(
        val data: ToastData,
    ) : MapSideEffect
}
