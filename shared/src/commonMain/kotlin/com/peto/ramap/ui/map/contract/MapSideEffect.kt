package com.peto.ramap.ui.map.contract

import com.peto.ramap.core.base.SideEffect

sealed interface MapSideEffect : SideEffect {
    data object ShowLoginGuide : MapSideEffect

    data object ShowAccountDeleteUnavailable : MapSideEffect

    data object ShowToast : MapSideEffect

    data object ShowHiddenShopSearchResult : MapSideEffect
}
