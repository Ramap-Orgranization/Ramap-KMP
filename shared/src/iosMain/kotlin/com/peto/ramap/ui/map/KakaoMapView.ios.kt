package com.peto.ramap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.MarkerCluster
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import com.peto.ramap.platform.permission.PermissionStatus
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun KakaoMapView(
    shops: RamenShops,
    focusShops: List<RamenShop>,
    focusNearestToCurrentLocation: Boolean,
    selectedShopId: String?,
    bounds: MapBounds,
    clusterBounds: MapBounds,
    myLocationRequestKey: Int,
    onBoundsChanged: (MapBounds) -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    modifier: Modifier,
) {
    val mapController =
        remember {
            IosKakaoMapController(
                onBoundsChanged = onBoundsChanged,
                onShopClick = onShopClick,
                onMyLocationChanged = onMyLocationChanged,
            )
        }
    val locationPermissionController =
        rememberLocationPermissionGenerator { result ->
            when (result) {
                PermissionStatus.Granted -> mapController.moveToMyLocation()
                PermissionStatus.Blocked -> onLocationPermissionBlocked()
                PermissionStatus.Denied -> Unit
            }
        }
    val markerCluster = remember { MarkerCluster() }
    var viewportSize by remember { mutableStateOf(MapViewportSize()) }

    UIKitView(
        modifier =
            modifier.onSizeChanged { size ->
                viewportSize =
                    MapViewportSize(
                        width = size.width,
                        height = size.height,
                    )
            },
        factory = {
            mapController.view
        },
        update = {
            val markers =
                markerCluster.clustering(
                    shops = shops,
                    bounds = clusterBounds,
                    viewportWidth = viewportSize.width,
                    viewportHeight = viewportSize.height,
                    visibleBounds = bounds,
                )
            mapController.updateMarkers(
                markers = markers,
            )
            mapController.updateFocusShops(
                shops = focusShops,
                focusNearestToCurrentLocation = focusNearestToCurrentLocation,
            )
        },
        properties =
            UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = false,
            ),
    )

    LaunchedEffect(myLocationRequestKey) {
        if (myLocationRequestKey > 0) {
            locationPermissionController.requestPermission()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapController.dispose()
        }
    }
}
