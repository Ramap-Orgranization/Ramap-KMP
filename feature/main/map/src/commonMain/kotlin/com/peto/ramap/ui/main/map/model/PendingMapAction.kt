package com.peto.ramap.ui.main.map.model

import com.peto.ramap.domain.model.shop.RamenShop

sealed interface PendingMapAction {
    data class ToggleBookmark(
        val shop: RamenShop,
    ) : PendingMapAction

    data class ToggleShopNotification(
        val shop: RamenShop,
    ) : PendingMapAction

    data class ToggleHidden(
        val shop: RamenShop,
    ) : PendingMapAction

    data object ToggleBookmarkedShops : PendingMapAction
}
