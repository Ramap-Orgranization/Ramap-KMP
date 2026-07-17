package com.peto.ramap.ui.main.event.contract

import com.peto.ramap.ui.base.SideEffect

sealed interface EventDetailSideEffect : SideEffect {
    data object EventUnavailable : EventDetailSideEffect

    data object RequestNotificationPermission : EventDetailSideEffect
}
