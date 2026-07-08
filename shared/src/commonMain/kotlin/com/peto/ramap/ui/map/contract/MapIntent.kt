package com.peto.ramap.ui.map.contract

import com.peto.ramap.core.base.Intent
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.ShopInformationField
import com.peto.ramap.ui.map.model.MapPersonalization

sealed interface MapIntent : Intent {
    data class OnBoundsChanged(
        val bounds: MapBounds,
    ) : MapIntent

    data class OnMyLocationChanged(
        val location: Location,
    ) : MapIntent

    data class OnShopSelected(
        val shop: RamenShop,
    ) : MapIntent

    data object OnShopDetailDismissed : MapIntent

    data object OnSearchResultsDismissed : MapIntent

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
        val shop: RamenShop,
        val wrongFields: Set<ShopInformationField>,
        val description: String,
    ) : MapIntent

    data class OnPersonalizationViewChanged(
        val view: MapPersonalization,
    ) : MapIntent

    data object OnKakaoLoginClicked : MapIntent

    data object OnLogoutClicked : MapIntent

    data object OnAccountDeleteClicked : MapIntent

    data object OnLocationPermissionBlocked : MapIntent
}
