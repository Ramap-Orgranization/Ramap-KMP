package com.peto.ramap.navigation

import androidx.compose.runtime.Composable
import com.peto.ramap.ui.hidden.HiddenShopListRoute
import com.peto.ramap.ui.main.map.MapRoute
import com.peto.ramap.ui.main.my.MyTabRoute

@Composable
fun AppRoute() {
    val navigationState = rememberNavigationState()

    NavigationRouter(
        currentRoute = navigationState.currentRoute,
        selectedTab = navigationState.selectedTab,
        onTabSelected = navigationState::selectTopLevelTab,
        mapContent = {
            MapRoute(
                selectedShop = navigationState.selectedShop,
                onSelectedShopHandled = navigationState::clearSelectedShop,
            )
        },
        myContent = {
            MyTabRoute(onHiddenShopsNavigate = navigationState::showHiddenShops)
        },
        hiddenContent = {
            HiddenShopListRoute(
                onBackClick = navigationState::pop,
                onShopClick = navigationState::showShopOnMap,
            )
        },
    )
}
