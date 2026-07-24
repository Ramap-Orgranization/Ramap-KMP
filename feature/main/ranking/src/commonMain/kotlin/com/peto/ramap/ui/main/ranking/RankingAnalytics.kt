package com.peto.ramap.ui.main.ranking

import com.peto.ramap.analytics.AnalyticsEvents
import com.peto.ramap.analytics.AnalyticsParams
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.domain.model.shop.AreaFilter

class RankingAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logShopSelected(
        shopId: String,
        shopName: String,
        hasCategory: Boolean,
    ) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.SHOP_SELECT,
            params =
                mapOf(
                    AnalyticsParams.SHOP_ID to shopId,
                    AnalyticsParams.SHOP_NAME to shopName,
                    AnalyticsParams.SOURCE to AnalyticsSource.RANKING,
                    AnalyticsParams.HAS_CATEGORY to hasCategory,
                ),
        )
    }

    fun logCategoryToggled(
        categoryId: String,
        enabled: Boolean,
    ) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.CATEGORY_FILTER_TOGGLE,
            params =
                mapOf(
                    AnalyticsParams.CATEGORY to categoryId,
                    AnalyticsParams.ENABLED to enabled,
                    AnalyticsParams.SOURCE to AnalyticsSource.RANKING,
                ),
        )
    }

    fun logAllCategoriesSelected() {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.CATEGORY_FILTER_ALL,
            params =
                mapOf(
                    AnalyticsParams.SOURCE to AnalyticsSource.RANKING,
                ),
        )
    }

    fun logAreaSelected(areaFilter: AreaFilter) {
        val areaName =
            when (areaFilter) {
                is AreaFilter.Nationwide -> NATIONWIDE
                is AreaFilter.Selected -> areaFilter.area.name
            }

        analyticsTracker.logEvent(
            name = AnalyticsEvents.AREA_FILTER_SELECT,
            params =
                mapOf(
                    AnalyticsParams.AREA to areaName,
                    AnalyticsParams.SOURCE to AnalyticsSource.RANKING,
                ),
        )
    }

    fun logBookmarkToggled(
        shopId: String,
        enabled: Boolean,
    ) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.BOOKMARK_TOGGLE,
            params =
                mapOf(
                    AnalyticsParams.SHOP_ID to shopId,
                    AnalyticsParams.ENABLED to enabled,
                    AnalyticsParams.SOURCE to AnalyticsSource.RANKING,
                ),
        )
    }

    fun logLoginGuideShown() {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.LOGIN_GUIDE_SHOW,
            params =
                mapOf(
                    AnalyticsParams.SOURCE to AnalyticsSource.RANKING_BOOKMARK,
                ),
        )
    }

    fun logNextPageRequested() {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.RANKING_PAGE_LOAD,
        )
    }

    fun logLoginStarted() {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.LOGIN_START,
            params =
                mapOf(
                    AnalyticsParams.METHOD to LOGIN_METHOD_KAKAO,
                    AnalyticsParams.SOURCE to AnalyticsSource.RANKING,
                ),
        )
    }

    fun logLoginSucceeded() {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.LOGIN_SUCCESS,
            params =
                mapOf(
                    AnalyticsParams.METHOD to LOGIN_METHOD_KAKAO,
                    AnalyticsParams.SOURCE to AnalyticsSource.RANKING,
                ),
        )
    }

    fun logLoginFailed() {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.LOGIN_FAILURE,
            params =
                mapOf(
                    AnalyticsParams.METHOD to LOGIN_METHOD_KAKAO,
                    AnalyticsParams.SOURCE to AnalyticsSource.RANKING,
                ),
        )
    }

    companion object {
        private const val NATIONWIDE = "nationwide"
        private const val LOGIN_METHOD_KAKAO = "kakao"
    }
}
