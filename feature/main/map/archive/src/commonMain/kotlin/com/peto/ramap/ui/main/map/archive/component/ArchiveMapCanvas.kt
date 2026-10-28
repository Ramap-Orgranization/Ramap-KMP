package com.peto.ramap.ui.main.map.archive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
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
