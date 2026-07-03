package com.peto.ramap.ui.map.contract

import com.peto.ramap.core.base.SideEffect

sealed interface MapSideEffect : SideEffect {
    data object ShowToast : MapSideEffect
}
