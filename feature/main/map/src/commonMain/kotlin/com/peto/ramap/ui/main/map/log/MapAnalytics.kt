package com.peto.ramap.ui.main.map.log

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.common.shop.BookmarkToggled
import com.peto.ramap.analytics.common.shop.ShopSelected
import com.peto.ramap.analytics.event.CategoryFilterToggled
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.main.map.log.event.HiddenShopToggled
import com.peto.ramap.ui.main.map.log.event.SearchResultSelected
import com.peto.ramap.ui.main.map.log.event.ShopShared
import com.peto.ramap.ui.main.map.log.event.SubscribedToggled
import com.peto.ramap.ui.main.map.log.event.ViewportLoadFailed

class MapAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logShopShared(shop: RamenShop) {
        analyticsTracker.logEvent(
            ShopShared(
                shopId = shop.id,
                shopName = shop.name,
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

    fun logSearchResultSelected(shop: PlaceSearchResult) {
        analyticsTracker.logEvent(SearchResultSelected(shopName = shop.name))
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
    ) {
        analyticsTracker.logEvent(
            SubscribedToggled(
                shopId = shop.id,
                shopName = shop.name,
                enabled = enabled,
                source = AnalyticsSource.MAP,
            ),
        )
    }

    fun logHiddenToggled(
        shop: RamenShop,
        enabled: Boolean,
    ) {
        analyticsTracker.logEvent(
            HiddenShopToggled(
                shopId = shop.id,
                shopName = shop.name,
                enabled = enabled,
                source = AnalyticsSource.MAP,
            ),
        )
    }

    fun logBookmarkToggled(
        shop: RamenShop,
        enabled: Boolean,
    ) {
        analyticsTracker.logEvent(
            BookmarkToggled(
                shopId = shop.id,
                shopName = shop.name,
                enabled = enabled,
                source = AnalyticsSource.MAP,
            ),
        )
    }
}
