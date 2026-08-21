package com.peto.ramap.ui.main.notice.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface OperatingNoticeSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
    ) : OperatingNoticeSideEffect
}
