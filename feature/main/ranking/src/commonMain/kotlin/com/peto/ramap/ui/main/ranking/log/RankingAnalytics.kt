package com.peto.ramap.ui.main.ranking.log

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.common.CategoryFilterToggled
import com.peto.ramap.analytics.common.shop.BookmarkToggled
import com.peto.ramap.analytics.common.shop.ShopSelected
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.main.ranking.log.event.AreaFilterSelected
import com.peto.ramap.ui.main.ranking.log.event.RankingPageLoad

class RankingAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logShopSelected(shop: RamenShop) {
        analyticsTracker.logEvent(
            ShopSelected(
                shopId = shop.id,
                shopName = shop.name,
                source = AnalyticsSource.RANKING,
            ),
        )
    }

    fun logCategoryToggled(
        categoryId: String,
        enabled: Boolean,
    ) {
        analyticsTracker.logEvent(
            CategoryFilterToggled(
                category = categoryId,
                enabled = enabled,
                source = AnalyticsSource.RANKING,
            ),
        )
    }

    fun logAreaSelected(areaFilter: AreaFilter) {
        analyticsTracker.logEvent(
            AreaFilterSelected(
                area = areaName(areaFilter),
                district = districtName(areaFilter),
                source = AnalyticsSource.RANKING,
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
                source = AnalyticsSource.RANKING,
            ),
        )
    }

    fun logNextPageRequested() {
        analyticsTracker.logEvent(RankingPageLoad)
    }

    private fun areaName(areaFilter: AreaFilter): String =
        when (areaFilter) {
            AreaFilter.Nationwide -> NATIONWIDE
            is AreaFilter.Province -> areaFilter.area.name
            is AreaFilter.District -> areaFilter.area.name
        }

    private fun districtName(areaFilter: AreaFilter): String? =
        when (areaFilter) {
            AreaFilter.Nationwide,
            is AreaFilter.Province,
            -> null
            is AreaFilter.District -> areaFilter.district.name
        }

    companion object {
        private const val NATIONWIDE = "nationwide"
    }
}
