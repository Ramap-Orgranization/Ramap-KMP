package com.peto.ramap.ui.main.event.contract

import com.peto.ramap.core.base.SideEffect
import com.peto.ramap.designsystem.toast.model.ToastData

sealed interface EventDetailSideEffect : SideEffect {
    data object EventUnavailable : EventDetailSideEffect

    data class ShowEventToast(
        val data: ToastData,
    ) : EventDetailSideEffect
}
