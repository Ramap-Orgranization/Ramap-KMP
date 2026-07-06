package com.peto.ramap.ui.map

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.MapView
import com.peto.ramap.core.config.MarkerConfig
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.MarkerCluster
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.platform.LocationProvider
import com.peto.ramap.platform.permission.LocationPermissionGenerator
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.cluster_marker
import ramap.shared.generated.resources.marker_ramen
import ramap.shared.generated.resources.my_location_marker

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
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val coroutineScope = rememberCoroutineScope()
    val mapView = remember { MapView(context) }
    val kakaoMapState = remember { mutableStateOf<KakaoMap?>(null) }
    var viewportSize by remember { mutableStateOf(MapViewportSize()) }
    var myLocation by remember { mutableStateOf<Location?>(null) }
    val markerBitmap = rememberRamenShopMarkerBitmap()
    val clusterMarkerBitmap = rememberClusterMarkerBitmap()
    val myLocationMarkerBitmap = rememberMyLocationMarkerBitmap()

    val markerCluster = remember { MarkerCluster() }
    val locationProvider = remember(context) { LocationProvider(context) }
    val boundsCalculator = remember { MapBoundsCalculator() }
    val cameraController = remember { KakaoCameraController() }
    val markerRenderer = remember { KakaoMarkerRenderer() }
    val lifecycleController =
        remember(mapView, boundsCalculator) {
            KakaoMapLifecycleController(
                mapView = mapView,
                boundsCalculator = boundsCalculator,
            )
        }

    val locationPermissionGenerator =
        rememberLocationPermissionGenerator { result ->
            when (result) {
                PermissionStatus.Granted -> {
                    val kakaoMap = kakaoMapState.value ?: return@rememberLocationPermissionGenerator
                    coroutineScope.launch {
                        locationProvider.position()?.let { location ->
                            myLocation = location
                            onMyLocationChanged(location)
                            cameraController.moveToLocation(
                                kakaoMap = kakaoMap,
                                location = location,
                            )
                        }
                    }
                }

                PermissionStatus.Blocked -> onLocationPermissionBlocked()
                PermissionStatus.Denied -> Unit
            }
        }

    BindMapViewLifecycle(
        controller = lifecycleController,
        lifecycle = lifecycle,
    )

    RenderInitialLocationEffect(
        kakaoMap = kakaoMapState.value,
        locationProvider = locationProvider,
        locationPermissionGenerator = locationPermissionGenerator,
        markerRenderer = markerRenderer,
        myLocationMarkerBitmap = myLocationMarkerBitmap,
        onLocationChanged = { location ->
            myLocation = location
            onMyLocationChanged(location)
        },
    )

    RenderMarkersEffect(
        kakaoMap = kakaoMapState.value,
        markerBitmap = markerBitmap,
        clusterMarkerBitmap = clusterMarkerBitmap,
        markerCluster = markerCluster,
        markerRenderer = markerRenderer,
        cameraController = cameraController,
        shops = shops,
        selectedShopId = selectedShopId,
        bounds = bounds,
        clusterBounds = clusterBounds,
        viewportSize = viewportSize,
        onShopClick = onShopClick,
    )

    RenderMyLocationEffect(
        kakaoMap = kakaoMapState.value,
        markerRenderer = markerRenderer,
        myLocationMarkerBitmap = myLocationMarkerBitmap,
        myLocation = myLocation,
    )

    FocusShopsEffect(
        kakaoMap = kakaoMapState.value,
        locationProvider = locationProvider,
        locationPermissionGenerator = locationPermissionGenerator,
        cameraController = cameraController,
        focusShops = focusShops,
        focusNearestToCurrentLocation = focusNearestToCurrentLocation,
        onMyLocationChanged = onMyLocationChanged,
    )

    RequestMyLocationEffect(
        kakaoMap = kakaoMapState.value,
        myLocationRequestKey = myLocationRequestKey,
        locationPermissionGenerator = locationPermissionGenerator,
    )

    KakaoMapAndroidView(
        mapView = mapView,
        lifecycle = lifecycle,
        lifecycleController = lifecycleController,
        onBoundsChanged = onBoundsChanged,
        onMapReady = { kakaoMap ->
            kakaoMapState.value = kakaoMap
        },
        modifier =
            modifier.onSizeChanged { size ->
                viewportSize =
                    MapViewportSize(
                        width = size.width,
                        height = size.height,
                    )
            },
    )
}

@Composable
private fun RenderInitialLocationEffect(
    kakaoMap: KakaoMap?,
    locationProvider: LocationProvider,
    locationPermissionGenerator: LocationPermissionGenerator,
    markerRenderer: KakaoMarkerRenderer,
    myLocationMarkerBitmap: Bitmap,
    onLocationChanged: (Location) -> Unit,
) {
    LaunchedEffect(kakaoMap, myLocationMarkerBitmap, locationPermissionGenerator) {
        if (kakaoMap == null) return@LaunchedEffect
        if (!locationPermissionGenerator.hasPermission()) return@LaunchedEffect

        val location = locationProvider.position() ?: return@LaunchedEffect

        onLocationChanged(location)
        markerRenderer.renderMyLocation(
            kakaoMap = kakaoMap,
            markerBitmap = myLocationMarkerBitmap,
            location = location,
        )
    }
}

@Composable
private fun RenderMarkersEffect(
    kakaoMap: KakaoMap?,
    markerBitmap: Bitmap,
    clusterMarkerBitmap: Bitmap,
    markerCluster: MarkerCluster,
    markerRenderer: KakaoMarkerRenderer,
    cameraController: KakaoCameraController,
    shops: RamenShops,
    selectedShopId: String?,
    bounds: MapBounds,
    clusterBounds: MapBounds,
    viewportSize: MapViewportSize,
    onShopClick: (RamenShop) -> Unit,
) {
    LaunchedEffect(kakaoMap, markerBitmap, clusterMarkerBitmap, shops, selectedShopId, bounds, clusterBounds, viewportSize, onShopClick) {
        if (kakaoMap == null) return@LaunchedEffect

        val markers =
            markerCluster.clustering(
                shops = shops,
                bounds = clusterBounds,
                viewportWidth = viewportSize.width,
                viewportHeight = viewportSize.height,
                visibleBounds = bounds,
            )

        markerRenderer.render(
            kakaoMap = kakaoMap,
            markerBitmap = markerBitmap,
            clusterMarkerBitmap = clusterMarkerBitmap,
            markers = markers,
            selectedShopId = selectedShopId,
            onShopClick = onShopClick,
            onClusterClick = { cluster ->
                cameraController.focusRamenShops(
                    kakaoMap = kakaoMap,
                    shops = cluster.shops,
                )
            },
        )
    }
}

@Composable
private fun RenderMyLocationEffect(
    kakaoMap: KakaoMap?,
    markerRenderer: KakaoMarkerRenderer,
    myLocationMarkerBitmap: Bitmap,
    myLocation: Location?,
) {
    LaunchedEffect(kakaoMap, myLocationMarkerBitmap, myLocation) {
        if (kakaoMap == null || myLocation == null) return@LaunchedEffect

        markerRenderer.renderMyLocation(
            kakaoMap = kakaoMap,
            markerBitmap = myLocationMarkerBitmap,
            location = myLocation,
        )
    }
}

@Composable
private fun FocusShopsEffect(
    kakaoMap: KakaoMap?,
    locationProvider: LocationProvider,
    locationPermissionGenerator: LocationPermissionGenerator,
    cameraController: KakaoCameraController,
    focusShops: List<RamenShop>,
    focusNearestToCurrentLocation: Boolean,
    onMyLocationChanged: (Location) -> Unit,
) {
    LaunchedEffect(kakaoMap, focusShops, focusNearestToCurrentLocation) {
        if (kakaoMap == null) return@LaunchedEffect

        val currentLocation =
            if (focusNearestToCurrentLocation && locationPermissionGenerator.hasPermission()) {
                locationProvider.position()?.also(onMyLocationChanged)
            } else {
                null
            }

        cameraController.focusRamenShops(
            kakaoMap = kakaoMap,
            shops = focusShops,
            currentLocation = currentLocation,
        )
    }
}

@Composable
private fun RequestMyLocationEffect(
    kakaoMap: KakaoMap?,
    myLocationRequestKey: Int,
    locationPermissionGenerator: LocationPermissionGenerator,
) {
    LaunchedEffect(kakaoMap, myLocationRequestKey, locationPermissionGenerator) {
        if (myLocationRequestKey == 0 || kakaoMap == null) return@LaunchedEffect

        locationPermissionGenerator.requestPermission()
    }
}

@Composable
private fun KakaoMapAndroidView(
    mapView: MapView,
    lifecycle: Lifecycle,
    lifecycleController: KakaoMapLifecycleController,
    onBoundsChanged: (MapBounds) -> Unit,
    onMapReady: (KakaoMap) -> Unit,
    modifier: Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = {
            lifecycleController.startMap(
                lifecycle = lifecycle,
                onMapReady = onMapReady,
                onBoundsChanged = onBoundsChanged,
            )
            mapView
        },
    )
}

@Composable
private fun rememberRamenShopMarkerBitmap(): Bitmap {
    val density = LocalDensity.current
    val markerPainter = painterResource(Res.drawable.marker_ramen)
    val factory = RamenShopMarkerBitmapFactory()

    return remember(markerPainter, density) {
        factory.create(
            painter = markerPainter,
            density = density,
            width = MarkerConfig.Single.WIDTH,
            height = MarkerConfig.Single.HEIGHT,
        )
    }
}

@Composable
private fun rememberClusterMarkerBitmap(): Bitmap {
    val density = LocalDensity.current
    val markerPainter = painterResource(Res.drawable.cluster_marker)
    val factory = RamenShopMarkerBitmapFactory()

    return remember(markerPainter, density) {
        factory.create(
            painter = markerPainter,
            density = density,
            width = CLUSTER_MARKER_SIZE,
            height = CLUSTER_MARKER_SIZE,
        )
    }
}

@Composable
private fun rememberMyLocationMarkerBitmap(): Bitmap {
    val density = LocalDensity.current
    val markerPainter = painterResource(Res.drawable.my_location_marker)
    val factory = RamenShopMarkerBitmapFactory()

    return remember(markerPainter, density) {
        factory.create(
            painter = markerPainter,
            density = density,
            width = MY_LOCATION_MARKER_SIZE,
            height = MY_LOCATION_MARKER_SIZE,
        )
    }
}

private const val MY_LOCATION_MARKER_SIZE = 16
private const val CLUSTER_MARKER_SIZE = 28

@Composable
private fun BindMapViewLifecycle(
    controller: KakaoMapLifecycleController,
    lifecycle: Lifecycle,
) {
    DisposableEffect(lifecycle, controller) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> controller.resume()
                    Lifecycle.Event.ON_PAUSE -> controller.pause()
                    Lifecycle.Event.ON_DESTROY -> controller.finish()
                    else -> Unit
                }
            }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            controller.finish()
        }
    }
}
