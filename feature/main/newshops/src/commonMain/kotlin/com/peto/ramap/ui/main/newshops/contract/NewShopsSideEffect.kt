package com.peto.ramap.ui.main.newshops.contract

import com.peto.ramap.ui.base.SideEffect

internal sealed interface NewShopsSideEffect : SideEffect {
    data object ShowRefreshFailure : NewShopsSideEffect
}
