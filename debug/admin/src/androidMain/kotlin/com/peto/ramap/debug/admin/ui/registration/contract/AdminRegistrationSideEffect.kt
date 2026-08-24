package com.peto.ramap.debug.admin.ui.registration.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

internal sealed interface AdminRegistrationSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
    ) : AdminRegistrationSideEffect
}
