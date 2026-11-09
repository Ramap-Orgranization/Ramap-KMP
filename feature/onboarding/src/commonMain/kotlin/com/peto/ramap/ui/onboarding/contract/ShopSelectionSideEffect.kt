package com.peto.ramap.ui.onboarding.contract

import com.peto.ramap.ui.base.SideEffect

internal sealed interface ShopSelectionSideEffect : SideEffect {
    data object Completed : ShopSelectionSideEffect
}
