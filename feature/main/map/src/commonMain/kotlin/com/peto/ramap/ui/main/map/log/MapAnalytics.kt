package com.peto.ramap.ui.main.map.log

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.common.CategoryFilterToggled
import com.peto.ramap.analytics.common.deeplink.ShareLinkClicked
import com.peto.ramap.analytics.common.shop.BookmarkToggled
import com.peto.ramap.analytics.common.shop.HiddenShopToggled
import com.peto.ramap.analytics.common.shop.ShopSelected
import com.peto.ramap.analytics.common.shop.SubscribedToggled
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.main.map.log.event.ShopMapLinkOpened
import com.peto.ramap.ui.main.map.log.event.ViewportLoadFailed

class MapAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logShopShared(shop: RamenShop) {
        analyticsTracker.logEvent(
            ShareLinkClicked(shopId = shop.id),
        )
    }

    fun logShopMapLinkOpened(
        shop: RamenShop,
        mapProvider: String,
    ) {
        analyticsTracker.logEvent(
            ShopMapLinkOpened(
                shopId = shop.id,
                shopName = shop.name,
                mapProvider = mapProvider,
            ),
        )
    }

    fun logViewportLoadError() {
        analyticsTracker.logEvent(ViewportLoadFailed)
    }

    fun logShopSelected(
        shop: RamenShop,
        source: AnalyticsSource,
    ) {
        analyticsTracker.logEvent(
            ShopSelected(
                shopId = shop.id,
                shopName = shop.name,
                source = source,
            ),
        )
    }

    fun logCategoryFilterToggled(
        category: Category,
        enabled: Boolean,
    ) {
        analyticsTracker.logEvent(
            CategoryFilterToggled(
                category = category.id,
                enabled = enabled,
                source = AnalyticsSource.MAP,
            ),
        )
    }

    fun logShopSubscribed(
        shop: RamenShop,
        enabled: Boolean,
        source: AnalyticsSource = AnalyticsSource.MAP,
    ) {
        analyticsTracker.logEvent(
            SubscribedToggled(
                shopId = shop.id,
                shopName = shop.name,
                enabled = enabled,
                source = source,
            ),
        )
    }

    fun logHiddenToggled(
        shop: RamenShop,
        enabled: Boolean,
        source: AnalyticsSource = AnalyticsSource.MAP,
    ) {
        analyticsTracker.logEvent(
            HiddenShopToggled(
                shopId = shop.id,
                shopName = shop.name,
                enabled = enabled,
                source = source,
            ),
        )
    }

    fun logBookmarkToggled(
        shop: RamenShop,
        enabled: Boolean,
        source: AnalyticsSource = AnalyticsSource.MAP,
    ) {
        analyticsTracker.logEvent(
            BookmarkToggled(
                shopId = shop.id,
                shopName = shop.name,
                enabled = enabled,
                source = source,
            ),
        )
    }
}
