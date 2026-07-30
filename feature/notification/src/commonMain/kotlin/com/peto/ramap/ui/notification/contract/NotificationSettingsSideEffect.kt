package com.peto.ramap.ui.notification.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface NotificationSettingsSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
    ) : NotificationSettingsSideEffect
}
