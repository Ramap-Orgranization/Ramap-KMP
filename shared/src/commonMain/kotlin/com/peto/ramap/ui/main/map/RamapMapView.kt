package com.peto.ramap.ui.main.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShops

@Composable
expect fun RamapMapView(
    shops: RamenShops,
    focusShops: List<RamenShop>,
    focusNearestToCurrentLocation: Boolean,
    focusRequestKey: Long,
    initialFocusLocation: Location?,
    initialFocusRequestKey: Long,
    shouldBootstrapInitialLocationFocus: Boolean,
    selectedShopId: String?,
    onMapMoveStarted: () -> Unit,
    onBoundsChanged: (MapBounds) -> Unit,
    onInitialFocusConsumed: () -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    modifier: Modifier = Modifier,
)
