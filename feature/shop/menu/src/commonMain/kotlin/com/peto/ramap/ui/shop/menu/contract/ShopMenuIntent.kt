package com.peto.ramap.ui.shop.menu.contract

import com.peto.ramap.ui.base.Intent

sealed interface ShopMenuIntent : Intent {
    data class OnShopSelected(
        val shopId: String,
    ) : ShopMenuIntent

    data object OnRetry : ShopMenuIntent
}
