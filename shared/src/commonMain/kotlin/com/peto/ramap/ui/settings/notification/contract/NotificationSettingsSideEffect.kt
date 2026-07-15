package com.peto.ramap.ui.settings.notification.contract

import com.peto.ramap.core.base.SideEffect
import com.peto.ramap.designsystem.toast.model.ToastData

sealed interface NotificationSettingsSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
    ) : NotificationSettingsSideEffect
}
