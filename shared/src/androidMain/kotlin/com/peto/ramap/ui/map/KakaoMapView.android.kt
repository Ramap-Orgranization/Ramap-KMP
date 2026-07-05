package com.peto.ramap.ui.map

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.marker_ramen

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
    val mapView = remember { MapView(context) }
    val kakaoMapState = remember { mutableStateOf<KakaoMap?>(null) }
    var isRequestingLocationPermission by remember { mutableStateOf(false) }
    var viewportSize by remember { mutableStateOf(MapViewportSize()) }
    var myLocation by remember { mutableStateOf<Location?>(null) }
    val markerBitmap = rememberRamenShopMarkerBitmap()

    val markerCluster = remember { MarkerCluster() }
    val locationProvider = remember(context) { LocationProvider(context) }
    val boundsCalculator = remember { MapBoundsCalculator() }
    val cameraController = remember { KakaoCameraController() }
    val markerRenderer = remember { KakaoMarkerRenderer(RamenShopClusterBitmapFactory()) }
    val lifecycleController =
        remember(mapView, boundsCalculator) {
            KakaoMapLifecycleController(
                mapView = mapView,
                boundsCalculator = boundsCalculator,
            )
        }

    val locationPermissionLauncher =
        rememberKakaoMapLocationPermissionLauncher(
            kakaoMapState = kakaoMapState,
            locationProvider = locationProvider,
            cameraController = cameraController,
            onLocationReceived = { location ->
                val domainLocation =
                    Location(
                        lat = location.latitude,
                        lng = location.longitude,
                    )
                myLocation = domainLocation
                onMyLocationChanged(domainLocation)
            },
            onLocationPermissionBlocked = {
                if (isRequestingLocationPermission) {
                    onLocationPermissionBlocked()
                    isRequestingLocationPermission = false
                }
            },
        )

    BindMapViewLifecycle(
        controller = lifecycleController,
        lifecycle = lifecycle,
    )

    RenderInitialLocationEffect(
        kakaoMap = kakaoMapState.value,
        locationProvider = locationProvider,
        markerRenderer = markerRenderer,
        onLocationChanged = { location ->
            myLocation = location
            onMyLocationChanged(location)
        },
    )

    RenderMarkersEffect(
        kakaoMap = kakaoMapState.value,
        markerBitmap = markerBitmap,
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
        myLocation = myLocation,
    )

    FocusShopsEffect(
        kakaoMap = kakaoMapState.value,
        locationProvider = locationProvider,
        cameraController = cameraController,
        focusShops = focusShops,
        focusNearestToCurrentLocation = focusNearestToCurrentLocation,
        onMyLocationChanged = onMyLocationChanged,
    )

    RequestMyLocationEffect(
        kakaoMap = kakaoMapState.value,
        myLocationRequestKey = myLocationRequestKey,
        locationProvider = locationProvider,
        cameraController = cameraController,
        locationPermissionLauncher = locationPermissionLauncher,
        onLocationReceived = { location ->
            myLocation = location
            onMyLocationChanged(location)
        },
        onLocationPermissionBlocked = onLocationPermissionBlocked,
        onPermissionResultSnackbarVisibilityChanged = { shouldShow ->
            isRequestingLocationPermission = shouldShow
        },
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
    markerRenderer: KakaoMarkerRenderer,
    onLocationChanged: (Location) -> Unit,
) {
    LaunchedEffect(kakaoMap) {
        if (kakaoMap == null) return@LaunchedEffect

        val location = locationProvider.currentLocation() ?: return@LaunchedEffect
        val domainLocation =
            Location(
                lat = location.latitude,
                lng = location.longitude,
            )

        onLocationChanged(domainLocation)
        markerRenderer.renderMyLocation(kakaoMap, domainLocation)
    }
}

@Composable
private fun RenderMarkersEffect(
    kakaoMap: KakaoMap?,
    markerBitmap: Bitmap,
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
    LaunchedEffect(kakaoMap, markerBitmap, shops, selectedShopId, bounds, clusterBounds, viewportSize, onShopClick) {
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
    myLocation: Location?,
) {
    LaunchedEffect(kakaoMap, myLocation) {
        if (kakaoMap == null || myLocation == null) return@LaunchedEffect

        markerRenderer.renderMyLocation(kakaoMap, myLocation)
    }
}

@Composable
private fun FocusShopsEffect(
    kakaoMap: KakaoMap?,
    locationProvider: LocationProvider,
    cameraController: KakaoCameraController,
    focusShops: List<RamenShop>,
    focusNearestToCurrentLocation: Boolean,
    onMyLocationChanged: (Location) -> Unit,
) {
    LaunchedEffect(kakaoMap, focusShops, focusNearestToCurrentLocation) {
        if (kakaoMap == null) return@LaunchedEffect

        val currentLocation =
            if (focusNearestToCurrentLocation) {
                locationProvider.currentLocation()?.also { location ->
                    onMyLocationChanged(
                        Location(
                            lat = location.latitude,
                            lng = location.longitude,
                        ),
                    )
                }
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
    locationProvider: LocationProvider,
    cameraController: KakaoCameraController,
    locationPermissionLauncher: ActivityResultLauncher<Array<String>>,
    onLocationReceived: (Location) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    onPermissionResultSnackbarVisibilityChanged: (Boolean) -> Unit,
) {
    LaunchedEffect(kakaoMap, myLocationRequestKey) {
        if (myLocationRequestKey == 0 || kakaoMap == null) return@LaunchedEffect

        onPermissionResultSnackbarVisibilityChanged(true)
        locationProvider.ensureLocationPermission(
            permissionLauncher = locationPermissionLauncher,
            onGranted = {
                onPermissionResultSnackbarVisibilityChanged(false)
                locationProvider
                    .moveToLastKnownLocation(
                        kakaoMap = kakaoMap,
                        cameraController = cameraController,
                    )?.let { location ->
                        onLocationReceived(
                            Location(
                                lat = location.latitude,
                                lng = location.longitude,
                            ),
                        )
                    }
            },
            onBlocked = {
                onLocationPermissionBlocked()
                onPermissionResultSnackbarVisibilityChanged(false)
            },
        )
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
private fun rememberKakaoMapLocationPermissionLauncher(
    kakaoMapState: MutableState<KakaoMap?>,
    locationProvider: LocationProvider,
    cameraController: KakaoCameraController,
    onLocationReceived: (android.location.Location) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
) { permissions ->
    if (locationProvider.isLocationGranted(permissions)) {
        val kakaoMap = kakaoMapState.value ?: return@rememberLauncherForActivityResult
        locationProvider
            .moveToLastKnownLocation(
                kakaoMap = kakaoMap,
                cameraController = cameraController,
            )?.let(onLocationReceived)
    } else if (locationProvider.isLocationPermissionBlocked()) {
        onLocationPermissionBlocked()
    }
}

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
