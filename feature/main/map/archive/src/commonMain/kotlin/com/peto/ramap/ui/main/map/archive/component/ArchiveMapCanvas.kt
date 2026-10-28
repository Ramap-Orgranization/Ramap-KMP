package com.peto.ramap.ui.main.map.archive.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.main.map.archive.ArchiveMapView
import com.peto.ramap.ui.main.map.archive.model.ArchiveMapRenderModel
import com.peto.ramap.ui.main.map.archive.model.CameraPosition
import com.peto.ramap.ui.main.map.archive.model.MapClusteringOptions
import com.peto.ramap.ui.main.map.archive.model.rememberArchiveMarkerImages

@Composable
internal fun ArchiveMapCanvas(
    shops: RamenShops,
    photos: Map<String, MyReviewPhoto>,
    failedPhotoReferences: Set<String>,
    clusteringOptions: MapClusteringOptions,
    selectedShopId: String?,
    cameraPosition: CameraPosition?,
    shouldFocusVisits: Boolean,
    onFocusConsumed: () -> Unit,
    onCameraChanged: (CameraPosition) -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onClusterClick: (List<RamenShop>, IntOffset?) -> Unit,
    onMapMoveStarted: () -> Unit,
    onPhotoDownloadFailed: (String, String) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    onCurrentLocationTimeout: () -> Unit,
    onInitialMarkersReady: () -> Unit,
    onMapReady: () -> Unit,
) {
    if (LocalInspectionMode.current) return
    val markerImages =
        rememberArchiveMarkerImages(
            shops = shops,
            photos = photos,
            failedPhotoReferences = failedPhotoReferences,
            onPhotoDownloadFailed = onPhotoDownloadFailed,
        )
    if (!rememberInitialMarkersReady(shops.keys, markerImages.keys)) return
    SideEffect { onInitialMarkersReady() }
    ArchiveMapView(
        request =
            ArchiveMapRenderModel(
                shops = shops.filterByShopIds(markerImages.keys),
                focusShops = shops,
                markerImages = markerImages,
                clusteringOptions = clusteringOptions,
                selectedShopId = selectedShopId,
                cameraPosition = cameraPosition,
                shouldFocusShops = shouldFocusVisits,
                onFocusConsumed = onFocusConsumed,
                onCameraChanged = onCameraChanged,
                onShopClick = onShopClick,
                onClusterClick = onClusterClick,
                onMapMoveStarted = onMapMoveStarted,
                onLocationPermissionBlocked = onLocationPermissionBlocked,
                onCurrentLocationTimeout = onCurrentLocationTimeout,
                onMapReady = onMapReady,
            ),
    )
}

@Composable
internal fun rememberInitialMarkersReady(
    shopIds: Set<String>,
    markerIds: Set<String>,
): Boolean {
    var hasInitialMarkers by remember { mutableStateOf(false) }
    val isReady = hasInitialMarkers || markerIds.containsAll(shopIds)
    SideEffect { if (isReady) hasInitialMarkers = true }
    return isReady
}
