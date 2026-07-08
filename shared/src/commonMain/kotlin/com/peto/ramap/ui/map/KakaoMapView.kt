package com.peto.ramap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShops

@Composable
expect fun KakaoMapView(
    shops: RamenShops,
    focusShops: List<RamenShop>,
    focusNearestToCurrentLocation: Boolean,
    focusRequestKey: Long,
    selectedShopId: String?,
    bounds: MapBounds,
    clusterBounds: MapBounds,
    myLocationRequestKey: Int,
    onBoundsChanged: (MapBounds) -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    modifier: Modifier = Modifier,
)
