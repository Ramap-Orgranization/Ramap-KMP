package com.peto.ramap.ui.profile.edit

import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.ui.base.SideEffect
import org.jetbrains.compose.resources.StringResource

sealed interface ProfileSideEffect : SideEffect {
    data object NavigateBack : ProfileSideEffect

    data class ShowToast(
        val message: StringResource,
        val type: ToastType = ToastType.DEFAULT,
    ) : ProfileSideEffect
}
