package com.peto.ramap.ui.shop.overview.contract

import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.base.SideEffect
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect

sealed interface ShopOverviewSideEffect : SideEffect {
    data class Interaction(
        val effect: ShopInteractionEffect,
    ) : ShopOverviewSideEffect

    data class ShopViewed(
        val shop: RamenShop,
    ) : ShopOverviewSideEffect

    data object SelectedShopHidden : ShopOverviewSideEffect
}
