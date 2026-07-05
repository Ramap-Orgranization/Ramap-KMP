package com.peto.ramap.ui.map

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    var shouldShowBlockedSnackbarOnPermissionResult by remember { mutableStateOf(false) }
    var viewportSize by remember { mutableStateOf(MapViewportSize()) }
    var myLocation by remember { mutableStateOf<Location?>(null) }
    val markerBitmap = rememberRamenShopMarkerBitmap()
    val currentOnShopClick = rememberUpdatedState(onShopClick)
    val currentOnLocationPermissionBlocked = rememberUpdatedState(onLocationPermissionBlocked)

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
                val domainLocation = location.toDomainLocation()
                myLocation = domainLocation
                onMyLocationChanged(domainLocation)
            },
            onLocationPermissionBlocked = {
                if (shouldShowBlockedSnackbarOnPermissionResult) {
                    currentOnLocationPermissionBlocked.value()
                    shouldShowBlockedSnackbarOnPermissionResult = false
                }
            },
        )

    BindMapViewLifecycle(
        controller = lifecycleController,
        lifecycle = lifecycle,
    )

    LaunchedEffect(kakaoMapState.value) {
        val kakaoMap = kakaoMapState.value ?: return@LaunchedEffect
        val location = locationProvider.currentLocation()?.toDomainLocation() ?: return@LaunchedEffect

        myLocation = location
        onMyLocationChanged(location)
        markerRenderer.renderMyLocation(kakaoMap, location)
    }

    LaunchedEffect(kakaoMapState.value, markerBitmap, shops, selectedShopId, bounds, clusterBounds, viewportSize) {
        val kakaoMap = kakaoMapState.value ?: return@LaunchedEffect
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
            onShopClick = { shop ->
                cameraController.moveToSelectedShop(kakaoMap, shop)
                currentOnShopClick.value(shop)
            },
            onClusterClick = { cluster ->
                cameraController.focusRamenShops(
                    kakaoMap = kakaoMap,
                    shops = cluster.shops,
                )
            },
        )
    }

    LaunchedEffect(kakaoMapState.value, myLocation) {
        val kakaoMap = kakaoMapState.value ?: return@LaunchedEffect
        val location = myLocation ?: return@LaunchedEffect

        markerRenderer.renderMyLocation(kakaoMap, location)
    }

    LaunchedEffect(kakaoMapState.value, focusShops, focusNearestToCurrentLocation) {
        val kakaoMap = kakaoMapState.value ?: return@LaunchedEffect
        cameraController.focusRamenShops(
            kakaoMap = kakaoMap,
            shops = focusShops,
            currentLocation =
                if (focusNearestToCurrentLocation) {
                    locationProvider.currentLocation()?.also { location ->
                        onMyLocationChanged(location.toDomainLocation())
                    }
                } else {
                    null
                },
        )
    }

    LaunchedEffect(kakaoMapState.value, myLocationRequestKey) {
        if (myLocationRequestKey == 0) return@LaunchedEffect

        val kakaoMap = kakaoMapState.value ?: return@LaunchedEffect
        shouldShowBlockedSnackbarOnPermissionResult = true
        locationProvider.ensureLocationPermission(
            permissionLauncher = locationPermissionLauncher,
            onGranted = {
                shouldShowBlockedSnackbarOnPermissionResult = false
                myLocation =
                    locationProvider
                        .moveToLastKnownLocation(
                            kakaoMap = kakaoMap,
                            cameraController = cameraController,
                        )?.toDomainLocation()
                        ?.also(onMyLocationChanged)
            },
            onBlocked = {
                currentOnLocationPermissionBlocked.value()
                shouldShowBlockedSnackbarOnPermissionResult = false
            },
        )
    }

    AndroidView(
        modifier =
            modifier.onSizeChanged { size ->
                viewportSize =
                    MapViewportSize(
                        width = size.width,
                        height = size.height,
                    )
            },
        factory = {
            lifecycleController.startMap(
                lifecycle = lifecycle,
                onMapReady = { kakaoMap ->
                    kakaoMapState.value = kakaoMap
                },
                onBoundsChanged = onBoundsChanged,
            )
            mapView
        },
    )
}

private fun android.location.Location.toDomainLocation(): Location =
    Location(
        lat = latitude,
        lng = longitude,
    )

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
