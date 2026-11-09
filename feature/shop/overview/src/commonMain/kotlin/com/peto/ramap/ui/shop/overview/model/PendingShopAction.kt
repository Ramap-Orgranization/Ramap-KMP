package com.peto.ramap.ui.shop.overview.model

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.domain.model.shop.RamenShop

sealed interface PendingShopAction {
    data class OpenNotificationSettings(
        val shop: RamenShop,
        val source: AnalyticsSource,
    ) : PendingShopAction
}
