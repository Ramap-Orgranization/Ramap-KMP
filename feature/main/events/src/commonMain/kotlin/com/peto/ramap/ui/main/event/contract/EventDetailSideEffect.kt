package com.peto.ramap.ui.main.event.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface EventDetailSideEffect : SideEffect {
    data object EventUnavailable : EventDetailSideEffect

    data class ShowEventToast(
        val data: ToastData,
    ) : EventDetailSideEffect
}
