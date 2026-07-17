package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface EventListSideEffect : SideEffect {
    data class ShowEventListToast(
        val data: ToastData,
    ) : EventListSideEffect
}
