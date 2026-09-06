package com.peto.ramap.ui.main.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.main.map.model.CameraPosition

@Composable
internal expect fun RamapMapView(
    shops: RamenShops,
    focusShops: RamenShops,
    focusNearestToCurrentLocation: Boolean,
    focusRequestKey: Long,
    initialFocusLocation: Location?,
    shouldBootstrapInitialLocationFocus: Boolean,
    selectedShopId: String?,
    cameraPosition: CameraPosition?,
    onMapMoveStarted: () -> Unit,
    onBoundsChanged: (MapBounds) -> Unit,
    onCameraPositionChanged: (CameraPosition) -> Unit,
    onInitialFocusConsumed: () -> Unit,
    onSelectedShopFocusConsumed: () -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onClusterClick: (List<RamenShop>, IntOffset?) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    onCurrentLocationTimeout: () -> Unit,
    modifier: Modifier = Modifier,
)
