package com.peto.ramap.ui.main.map

import com.peto.ramap.analytics.AnalyticsEvents
import com.peto.ramap.analytics.AnalyticsParams
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.report.ShopInformationReport
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.usecase.ShopDetail

class MapAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logViewportLoadError() {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.VIEWPORT_LOAD_ERROR,
        )
    }

    fun logShopSelected(shop: RamenShop) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.SHOP_SELECT,
            params =
                mapOf(
                    AnalyticsParams.SHOP_ID to shop.id,
                    AnalyticsParams.SHOP_NAME to shop.name,
                    AnalyticsParams.SOURCE to AnalyticsSource.MARKER,
                    AnalyticsParams.HAS_CATEGORY to shop.hasCategory,
                ),
        )
    }

    fun logShopDetailViewed(detail: ShopDetail) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.SHOP_DETAIL_VIEW,
            params =
                mapOf(
                    AnalyticsParams.SHOP_ID to detail.shop.id,
                    AnalyticsParams.CATEGORY_COUNT to
                        detail.shop.menuCategories.size,
                ),
        )
    }

    fun logPlaceSelected(place: PlaceSearchResult) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.SEARCH_PLACE_SELECT,
            params =
                mapOf(
                    AnalyticsParams.PLACE_NAME to place.name,
                ),
        )
    }

    fun logCategoryFilterToggled(
        category: Category,
        enabled: Boolean,
    ) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.CATEGORY_FILTER_TOGGLE,
            params =
                mapOf(
                    AnalyticsParams.CATEGORY to category.id,
                    AnalyticsParams.ENABLED to enabled,
                    AnalyticsParams.SOURCE to AnalyticsSource.MAP,
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
                    AnalyticsParams.SOURCE to AnalyticsSource.MAP,
                ),
        )
    }

    fun logShopNotificationToggled(
        shopId: String,
        enabled: Boolean,
    ) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.SHOP_NOTIFICATION_TOGGLE,
            params =
                mapOf(
                    AnalyticsParams.SHOP_ID to shopId,
                    AnalyticsParams.ENABLED to enabled,
                    AnalyticsParams.SOURCE to AnalyticsSource.MAP,
                ),
        )
    }

    fun logShopHiddenToggled(
        shopId: String,
        hidden: Boolean,
    ) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.SHOP_HIDE_TOGGLE,
            params =
                mapOf(
                    AnalyticsParams.SHOP_ID to shopId,
                    AnalyticsParams.ENABLED to hidden,
                    AnalyticsParams.SOURCE to AnalyticsSource.MAP,
                ),
        )
    }

    fun logBookmarkedViewToggled(enabled: Boolean) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.BOOKMARKED_VIEW_TOGGLE,
            params =
                mapOf(
                    AnalyticsParams.ENABLED to enabled,
                    AnalyticsParams.SOURCE to AnalyticsSource.MAP,
                ),
        )
    }

    fun logShopReportSubmitted(report: ShopInformationReport) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.SHOP_REPORT_SUBMIT,
            params =
                mapOf(
                    AnalyticsParams.SHOP_ID to report.shopId,
                    AnalyticsParams.WRONG_FIELD_COUNT to
                        report.wrongFields.size,
                    AnalyticsParams.HAS_DESCRIPTION to
                        report.description.isNotBlank(),
                ),
        )
    }

    fun logLoginStarted() {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.LOGIN_START,
            params =
                mapOf(
                    AnalyticsParams.METHOD to LOGIN_METHOD_KAKAO,
                    AnalyticsParams.SOURCE to AnalyticsSource.MAP,
                ),
        )
    }

    fun logLoginSucceeded() {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.LOGIN_SUCCESS,
            params =
                mapOf(
                    AnalyticsParams.METHOD to LOGIN_METHOD_KAKAO,
                    AnalyticsParams.SOURCE to AnalyticsSource.MAP,
                ),
        )
    }

    fun logLoginFailed() {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.LOGIN_FAILURE,
            params =
                mapOf(
                    AnalyticsParams.METHOD to LOGIN_METHOD_KAKAO,
                    AnalyticsParams.SOURCE to AnalyticsSource.MAP,
                ),
        )
    }

    companion object {
        private const val LOGIN_METHOD_KAKAO = "kakao"
    }
}
