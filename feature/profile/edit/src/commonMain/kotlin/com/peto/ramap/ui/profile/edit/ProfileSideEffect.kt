package com.peto.ramap.ui.profile.edit

import com.peto.ramap.ui.base.SideEffect
import org.jetbrains.compose.resources.StringResource

sealed interface ProfileSideEffect : SideEffect {
    data object NavigateBack : ProfileSideEffect

    data class Toast(
        val message: StringResource,
    ) : ProfileSideEffect
}
