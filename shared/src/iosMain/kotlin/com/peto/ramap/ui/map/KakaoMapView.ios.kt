package com.peto.ramap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.peto.ramap.platform.LocationProvider
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()
    val locationProvider = remember { LocationProvider() }
    var myLocation by remember { mutableStateOf<Location?>(null) }
    val mapController =
        remember {
            IosKakaoMapController(
                onBoundsChanged = onBoundsChanged,
                onShopClick = onShopClick,
                onMyLocationChanged = onMyLocationChanged,
            )
        }
    val locationPermissionGenerator =
        rememberLocationPermissionGenerator { result ->
            when (result) {
                PermissionStatus.Granted -> {
                    coroutineScope.launch {
                        locationProvider.position()?.let { location ->
                            myLocation = location
                            mapController.moveToLocation(location)
                        }
                    }
                }

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
                currentLocation = myLocation.takeIf { focusNearestToCurrentLocation },
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
            locationPermissionGenerator.requestPermission()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapController.dispose()
        }
    }
}
