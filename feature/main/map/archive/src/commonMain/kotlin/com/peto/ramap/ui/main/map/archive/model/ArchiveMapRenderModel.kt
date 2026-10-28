package com.peto.ramap.ui.main.map.archive.model

import androidx.compose.ui.unit.IntOffset
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops

internal data class ArchiveMapRenderModel(
    val shops: RamenShops,
    val focusShops: RamenShops,
    val markerImages: Map<String, MapMarkerImage>,
    val clusteringOptions: MapClusteringOptions,
    val selectedShopId: String?,
    val cameraPosition: CameraPosition?,
    val shouldFocusShops: Boolean,
    val onFocusConsumed: () -> Unit,
    val onCameraChanged: (CameraPosition) -> Unit,
    val onShopClick: (RamenShop) -> Unit,
    val onClusterClick: (List<RamenShop>, IntOffset?) -> Unit,
    val onMapMoveStarted: () -> Unit,
    val onLocationPermissionBlocked: () -> Unit,
    val onCurrentLocationTimeout: () -> Unit,
    val onMapReady: () -> Unit,
)
