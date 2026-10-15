package com.peto.ramap.ui.shop.model

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.domain.model.shop.RamenShop

sealed interface PendingShopAction {
    data object LoginRequest : PendingShopAction

    data class ToggleBookmark(
        val shop: RamenShop,
        val source: AnalyticsSource,
        val enabled: Boolean,
    ) : PendingShopAction

    data class ToggleShopNotification(
        val shop: RamenShop,
        val source: AnalyticsSource,
    ) : PendingShopAction

    data class ToggleHidden(
        val shop: RamenShop,
        val source: AnalyticsSource,
    ) : PendingShopAction
}
