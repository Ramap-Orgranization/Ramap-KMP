package com.peto.ramap.ui.map.contract

import com.peto.ramap.core.base.SideEffect
import com.peto.ramap.designsystem.toast.model.ToastData

sealed interface MapSideEffect : SideEffect {
    data object ShowLoginGuide : MapSideEffect

    data class ShowToast(
        val data: ToastData,
    ) : MapSideEffect
}
