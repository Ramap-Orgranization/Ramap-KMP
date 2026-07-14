package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.core.base.Intent
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.ShopInformationField
import com.peto.ramap.ui.main.map.model.MapPersonalization
import com.peto.ramap.ui.main.map.model.TabStatus

sealed interface MapIntent : Intent {
    data class OnBoundsChanged(
        val bounds: MapBounds,
    ) : MapIntent

    data class OnMyLocationChanged(
        val location: Location,
    ) : MapIntent

    data object OnInitialLocationFocusConsumed : MapIntent

    data class OnShopSelected(
        val shop: RamenShop,
    ) : MapIntent

    data class OnShopIdSelected(
        val shopId: String,
    ) : MapIntent

    data object OnShopDetailDismissed : MapIntent

    data object OnSearchResultsDismissed : MapIntent

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

    data class OnTopLevelTabChanged(
        val tab: TabStatus,
    ) : MapIntent

    data object OnBookmarkedShopsToggled : MapIntent

    data object OnKakaoLoginClicked : MapIntent

    data object OnLogoutClicked : MapIntent

    data object OnAccountDeleteConfirmed : MapIntent

    data object OnLocationPermissionBlocked : MapIntent
}
