package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.base.Intent
import com.peto.ramap.ui.main.map.model.CameraPosition
import com.peto.ramap.ui.main.map.model.PlaceFocusRequest
import com.peto.ramap.ui.main.map.model.ShopFocusRequest

sealed interface MapIntent : Intent {
    data class OnLoginTypeSelected(
        val type: LoginType,
    ) : MapIntent

    data class OnArchiveLoginTypeSelected(
        val type: LoginType,
    ) : MapIntent

    data object OnLoginSelectionDismissed : MapIntent

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

    data class OnSelectedShopFocusConsumed(
        val request: ShopFocusRequest,
    ) : MapIntent

    data class OnPlaceSelected(
        val place: PlaceSearchResult,
    ) : MapIntent

    data class OnPlaceFocusConsumed(
        val request: PlaceFocusRequest,
    ) : MapIntent

    data object OnMapTabExited : MapIntent

    data object OnOperatingNoticesRefreshRequested : MapIntent

    data object OnStatusTimeRefreshed : MapIntent

    data class OnShopSelected(
        val shop: RamenShop,
        val shouldFocus: Boolean = true,
        val source: AnalyticsSource = AnalyticsSource.MARKER,
    ) : MapIntent

    data class OnShopIdSelected(
        val shopId: String,
        val showDetail: Boolean = true,
    ) : MapIntent

    /** 다른 화면의 지도 보기로 진입할 때 이전 지도의 검색과 필터를 초기화한다. */
    data class OnShopNavigationRequested(
        val shopId: String,
        val showDetail: Boolean = true,
    ) : MapIntent

    data object OnRequestedShopDismissed : MapIntent

    data object OnShopDetailDismissed : MapIntent

    data object OnSearchResultsDismissed : MapIntent

    data class OnQueryChanged(
        val query: String,
    ) : MapIntent

    data class OnRecentSearchSelected(
        val query: String,
    ) : MapIntent

    data class OnRecentSearchDeleted(
        val query: String,
    ) : MapIntent

    data object OnRecentSearchesCleared : MapIntent

    data class OnCategoryFilterToggled(
        val category: Category,
    ) : MapIntent

    data object OnOpenFilterToggled : MapIntent

    data object OnBookmarkedShopsToggled : MapIntent

    data object OnLocationPermissionBlocked : MapIntent

    data class OnShopLoadFailed(
        val shopId: String,
    ) : MapIntent

    data class OnShopViewed(
        val shop: RamenShop,
    ) : MapIntent

    data object OnSelectedShopHidden : MapIntent
}
