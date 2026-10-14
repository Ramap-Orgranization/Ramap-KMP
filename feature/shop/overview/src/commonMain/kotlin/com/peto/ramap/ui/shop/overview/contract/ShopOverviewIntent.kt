package com.peto.ramap.ui.shop.overview.contract

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.base.Intent

sealed interface ShopOverviewIntent : Intent {
    data object OnLoginRequested : ShopOverviewIntent

    data class OnShopSelected(
        val shop: RamenShop,
    ) : ShopOverviewIntent

    data class OnShopShareClicked(
        val shop: RamenShop,
    ) : ShopOverviewIntent

    data class OnShopMapLinkClicked(
        val shop: RamenShop,
        val mapProvider: String,
    ) : ShopOverviewIntent

    data class OnShopIdSelected(
        val shopId: String,
    ) : ShopOverviewIntent

    data object OnRequestedShopDismissed : ShopOverviewIntent

    data object OnShopDetailDismissed : ShopOverviewIntent

    data object OnShopDetailRetry : ShopOverviewIntent

    data class OnBookmarkToggled(
        val shop: RamenShop,
        val source: AnalyticsSource = AnalyticsSource.MAP,
    ) : ShopOverviewIntent

    data class OnShopNotificationToggled(
        val shop: RamenShop,
        val source: AnalyticsSource = AnalyticsSource.MAP,
    ) : ShopOverviewIntent

    data class OnHiddenToggled(
        val shop: RamenShop,
        val source: AnalyticsSource = AnalyticsSource.MAP,
    ) : ShopOverviewIntent

    data class OnShopReportSubmitted(
        val wrongFields: Set<ShopInformationField>,
        val description: String,
    ) : ShopOverviewIntent

    data class OnLoginTypeSelected(
        val type: LoginType,
    ) : ShopOverviewIntent

    data object OnLoginSelectionDismissed : ShopOverviewIntent

    data class OnMapEntryChanged(
        val generation: Long,
    ) : ShopOverviewIntent

    data class OnLoginRequestedForEntry(
        val generation: Long,
    ) : ShopOverviewIntent

    data object OnPendingLoginActionCancelled : ShopOverviewIntent
}
