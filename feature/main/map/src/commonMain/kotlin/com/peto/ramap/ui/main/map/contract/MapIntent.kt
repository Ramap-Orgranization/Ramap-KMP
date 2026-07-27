package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.base.Intent
import com.peto.ramap.ui.main.map.model.CameraPosition

sealed interface MapIntent : Intent {
    data class OnBoundsChanged(
        val bounds: MapBounds,
    ) : MapIntent

    data object OnViewportLoadRetry : MapIntent

    data class OnCameraPositionChanged(
        val position: CameraPosition,
    ) : MapIntent

    data class OnMyLocationChanged(
        val location: Location,
    ) : MapIntent

    data object OnInitialLocationFocusConsumed : MapIntent

    data object OnSelectedShopFocusConsumed : MapIntent

    data object OnMapTabExited : MapIntent

    data class OnShopSelected(
        val shop: RamenShop,
        val shouldFocus: Boolean = true,
        val source: AnalyticsSource = AnalyticsSource.MARKER,
    ) : MapIntent

    data class OnShopShareClicked(
        val shop: RamenShop,
    ) : MapIntent

    data class OnShopIdSelected(
        val shopId: String,
    ) : MapIntent

    data object OnRequestedShopDismissed : MapIntent

    data object OnShopDetailDismissed : MapIntent

    data object OnShopDetailRetry : MapIntent

    data object OnSearchResultsDismissed : MapIntent

    data class OnSearchedShopSelected(
        val place: PlaceSearchResult,
    ) : MapIntent

    data class OnQueryChanged(
        val query: String,
    ) : MapIntent

    data class OnCategoryFilterToggled(
        val category: Category,
    ) : MapIntent

    data class OnBookmarkToggled(
        val shop: RamenShop,
    ) : MapIntent

    data class OnShopNotificationToggled(
        val shop: RamenShop,
    ) : MapIntent

    data class OnHiddenToggled(
        val shop: RamenShop,
    ) : MapIntent

    data class OnShopReportSubmitted(
        val wrongFields: Set<ShopInformationField>,
        val description: String,
    ) : MapIntent

    data object OnBookmarkedShopsToggled : MapIntent

    data object OnKakaoLoginClicked : MapIntent

    data object OnLocationPermissionBlocked : MapIntent
}
