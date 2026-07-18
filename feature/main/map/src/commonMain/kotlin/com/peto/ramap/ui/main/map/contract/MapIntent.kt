package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.base.Intent
import com.peto.ramap.ui.main.map.model.MapCameraPosition
import com.peto.ramap.ui.main.map.model.MapPersonalization

sealed interface MapIntent : Intent {
    data class OnBoundsChanged(
        val bounds: MapBounds,
    ) : MapIntent

    data class OnCameraPositionChanged(
        val position: MapCameraPosition,
    ) : MapIntent

    data class OnMyLocationChanged(
        val location: Location,
    ) : MapIntent

    data object OnInitialLocationFocusConsumed : MapIntent

    data class OnShopSelected(
        val shop: RamenShop,
        val shouldFocus: Boolean = true,
    ) : MapIntent

    data class OnShopIdSelected(
        val shopId: String,
    ) : MapIntent

    data object OnShopDetailDismissed : MapIntent

    data object OnSearchResultsDismissed : MapIntent

    data class OnPlaceSelected(
        val place: PlaceSearchResult,
    ) : MapIntent

    data object OnInitialMapRetryClicked : MapIntent

    data class OnQueryChanged(
        val query: String,
    ) : MapIntent

    data class OnCategoryFilterToggled(
        val category: Category,
    ) : MapIntent

    data object OnFilterCleared : MapIntent

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

    data class OnUnregisteredPlaceReportSubmitted(
        val placeUrl: String,
    ) : MapIntent

    data object OnCurrentLocationReportSubmitted : MapIntent

    data class OnPersonalizationViewChanged(
        val view: MapPersonalization,
    ) : MapIntent

    data object OnBookmarkedShopsToggled : MapIntent

    data object OnKakaoLoginClicked : MapIntent

    data object OnLogoutClicked : MapIntent

    data object OnAccountDeleteConfirmed : MapIntent

    data object OnLocationPermissionBlocked : MapIntent
}
