package com.peto.ramap.ui.main.event.detail.contract

import com.peto.ramap.ui.base.SideEffect
import org.jetbrains.compose.resources.StringResource

sealed interface EventDetailSideEffect : SideEffect {
    data object EventUnavailable : EventDetailSideEffect

    data object RequestNotificationPermission : EventDetailSideEffect

    data class ShowToast(
        val message: StringResource,
    ) : EventDetailSideEffect
}
