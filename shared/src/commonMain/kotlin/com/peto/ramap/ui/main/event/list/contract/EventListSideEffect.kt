package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.core.base.SideEffect
import com.peto.ramap.designsystem.toast.model.ToastData

sealed interface EventListSideEffect : SideEffect {
    data class ShowEventListToast(
        val data: ToastData,
    ) : EventListSideEffect
}
