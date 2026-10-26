package com.peto.ramap.ui.main.community.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect
import org.jetbrains.compose.resources.StringResource

sealed interface CommunitySideEffect : SideEffect {
    data class LoginRequired(
        val description: StringResource,
    ) : CommunitySideEffect

    data class ShowToast(
        val data: ToastData,
    ) : CommunitySideEffect
}
