package com.peto.ramap.ui.account.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface AccountSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
    ) : AccountSideEffect
}
