package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.ui.base.SideEffect
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect

sealed interface MapSideEffect : SideEffect {
    data class Interaction(
        val effect: ShopInteractionEffect,
    ) : MapSideEffect
}
