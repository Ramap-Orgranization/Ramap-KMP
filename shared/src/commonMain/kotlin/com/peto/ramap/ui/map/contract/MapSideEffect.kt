package com.peto.ramap.ui.map.contract

import com.peto.ramap.core.base.SideEffect
import org.jetbrains.compose.resources.StringResource

sealed interface MapSideEffect : SideEffect {
    data object ShowLoginGuide : MapSideEffect

    data object ShowLocationPermissionBlockedToast : MapSideEffect

    data class ShowToast(
        val messageResource: StringResource,
    ) : MapSideEffect
}
