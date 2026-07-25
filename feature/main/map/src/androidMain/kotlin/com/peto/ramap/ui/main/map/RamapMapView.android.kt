package com.peto.ramap.ui.main.map

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.util.FusedLocationSource
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.platform.permission.findActivity
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import com.peto.ramap.ui.main.map.component.LocationButton
import com.peto.ramap.ui.main.map.config.CurrentLocationConfig
import com.peto.ramap.ui.main.map.config.DefaultMapConfig
import com.peto.ramap.ui.main.map.config.MapInteractionConfig
import com.peto.ramap.ui.main.map.config.MarkerConfig
import com.peto.ramap.ui.main.map.model.CameraPosition
import com.peto.ramap.ui.main.map.model.CurrentLocationRequestState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.marker_ramen

@Composable
internal actual fun RamapMapView(
    shops: RamenShops,
    focusShops: RamenShops,
    focusNearestToCurrentLocation: Boolean,
    focusRequestKey: Long,
    initialFocusLocation: Location?,
    placeFocusLocation: Location?,
    placeFocusRequestKey: Long,
    shouldBootstrapInitialLocationFocus: Boolean,
    selectedShopId: String?,
    cameraPosition: CameraPosition?,
    onMapMoveStarted: () -> Unit,
    onBoundsChanged: (MapBounds) -> Unit,
    onCameraPositionChanged: (CameraPosition) -> Unit,
    onInitialFocusConsumed: () -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    onCurrentLocationTimeout: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    var viewportHeight by remember { mutableStateOf(0) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var shouldMoveToCurrentLocation by remember { mutableStateOf(false) }
    var currentLocationRequestState by remember {
        mutableStateOf(CurrentLocationRequestState.Idle)
    }
    var isCameraMoving = false
    val locationSource =
        remember(context) {
            findActivity(context)?.let { activity ->
                FusedLocationSource(activity, LOCATION_PERMISSION_REQUEST_CODE)
            }
        }
    val cameraController = remember { NaverCameraController() }
    val moveToCurrentLocation = {
        val map = naverMap
        val location = currentLocation
        if (map != null && location != null) {
            map.locationTrackingMode = LocationTrackingMode.NoFollow
            map.moveCamera(
                CameraUpdate.scrollAndZoomTo(
                    LatLng(location.lat, location.lng),
                    CurrentLocationConfig.zoomForCurrentLocation(map.cameraPosition.zoom),
                ),
            )
            shouldMoveToCurrentLocation = false
            currentLocationRequestState = currentLocationRequestState.finish()
        }
    }
    val markerBitmap = rememberMarkerBitmap()
    val clusterRenderer =
        remember(markerBitmap, onShopClick) {
            NaverShopClusterRenderer(
                markerBitmap = markerBitmap,
                onShopClick = onShopClick,
            )
        }

    val locationPermissionGenerator =
        rememberLocationPermissionGenerator { result ->
            when (result) {
                PermissionStatus.Granted -> {
                    if (shouldBootstrapInitialLocationFocus) {
                        naverMap?.locationTrackingMode = LocationTrackingMode.Follow
                    }
                    if (shouldMoveToCurrentLocation) {
                        naverMap?.locationTrackingMode = LocationTrackingMode.NoFollow
                        moveToCurrentLocation()
                    }
                }
                PermissionStatus.Blocked -> {
                    shouldMoveToCurrentLocation = false
                    currentLocationRequestState = currentLocationRequestState.finish()
                    onLocationPermissionBlocked()
                }
                PermissionStatus.Denied -> {
                    shouldMoveToCurrentLocation = false
                    currentLocationRequestState = currentLocationRequestState.finish()
                }
            }
        }

    BindNaverMapLifecycle(mapView, lifecycle)

    LaunchedEffect(naverMap, shops, clusterRenderer) {
        val map = naverMap ?: return@LaunchedEffect
        clusterRenderer.render(map, shops)
    }

    LaunchedEffect(
        naverMap,
        initialFocusLocation,
    ) {
        val map = naverMap ?: return@LaunchedEffect
        val location = initialFocusLocation ?: return@LaunchedEffect
        cameraController.focusCurrentLocation(
            naverMap = map,
            location = location,
        )
        map.locationTrackingMode = LocationTrackingMode.NoFollow
        onInitialFocusConsumed()
    }

    LaunchedEffect(
        naverMap,
        placeFocusLocation,
        placeFocusRequestKey,
    ) {
        val map = naverMap ?: return@LaunchedEffect
        val location = placeFocusLocation ?: return@LaunchedEffect
        cameraController.focusPlace(
            naverMap = map,
            location = location,
            requestKey = placeFocusRequestKey,
        )
    }

    LaunchedEffect(
        naverMap,
        focusShops,
        focusNearestToCurrentLocation,
        focusRequestKey,
        selectedShopId,
        viewportHeight,
        currentLocation,
    ) {
        val map = naverMap ?: return@LaunchedEffect
        map.setContentPadding(
            0,
            0,
            0,
            if (selectedShopId != null && focusShops.size == 1) {
                (viewportHeight * MapInteractionConfig.SELECTED_MARKER_BOTTOM_PADDING_RATIO).toInt()
            } else {
                0
            },
        )
        cameraController.focusShops(
            map,
            focusShops,
            currentLocation.takeIf { focusNearestToCurrentLocation },
            focusRequestKey,
        )
    }

    LaunchedEffect(naverMap, shouldBootstrapInitialLocationFocus) {
        if (naverMap != null) {
            locationPermissionGenerator.requestPermission()
            if (locationPermissionGenerator.hasPermission() && shouldBootstrapInitialLocationFocus) {
                naverMap?.locationTrackingMode = LocationTrackingMode.Follow
            }
        }
    }

    LaunchedEffect(currentLocationRequestState) {
        if (!currentLocationRequestState.isLoading) return@LaunchedEffect
        delay(CurrentLocationConfig.REQUEST_TIMEOUT_MILLIS)
        if (!currentLocationRequestState.isLoading) return@LaunchedEffect

        shouldMoveToCurrentLocation = false
        currentLocationRequestState = currentLocationRequestState.timeout()
        onCurrentLocationTimeout()
    }

    Box(modifier = modifier.onSizeChanged { viewportHeight = it.height }) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.getMapAsync(
                    OnMapReadyCallback { map ->
                        map.minZoom = MapInteractionConfig.MAX_ZOOM_OUT_LEVEL.toDouble()
                        map.locationSource = locationSource
                        map.uiSettings.isCompassEnabled = false
                        map.uiSettings.isLocationButtonEnabled = false
                        map.uiSettings.isZoomControlEnabled = false
                        map.addOnLocationChangeListener { location ->
                            Location(location.latitude, location.longitude).let { current ->
                                currentLocation = current
                                onMyLocationChanged(current)
                                if (shouldMoveToCurrentLocation) moveToCurrentLocation()
                            }
                        }
                        map.addOnCameraChangeListener { _, _ ->
                            if (!isCameraMoving) {
                                isCameraMoving = true
                                onMapMoveStarted()
                            }
                        }
                        map.addOnCameraIdleListener {
                            isCameraMoving = false
                            notifyBounds(map, onBoundsChanged)
                            notifyCameraPosition(map, onCameraPositionChanged)
                        }
                        val initialCenter =
                            cameraPosition?.center
                                ?: Location(DefaultMapConfig.LATITUDE, DefaultMapConfig.LONGITUDE)
                        map.moveCamera(
                            CameraUpdate.scrollAndZoomTo(
                                LatLng(initialCenter.lat, initialCenter.lng),
                                cameraPosition?.zoom ?: DefaultMapConfig.ZOOM_LEVEL.toDouble(),
                            ),
                        )
                        naverMap = map
                        mapView.post { notifyBounds(map, onBoundsChanged) }
                    },
                )
                mapView
            },
        )

        LocationButton(
            isLoading = currentLocationRequestState.isLoading,
            onClick = {
                if (currentLocationRequestState.isLoading) return@LocationButton
                currentLocationRequestState = currentLocationRequestState.start()
                if (locationPermissionGenerator.hasPermission()) {
                    shouldMoveToCurrentLocation = true
                    naverMap?.locationTrackingMode = LocationTrackingMode.NoFollow
                    moveToCurrentLocation()
                } else {
                    shouldMoveToCurrentLocation = true
                    locationPermissionGenerator.requestPermission()
                }
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 16.dp),
        )
    }

    DisposableEffect(clusterRenderer) {
        onDispose { clusterRenderer.dispose() }
    }
}

private fun notifyCameraPosition(
    naverMap: NaverMap,
    onCameraPositionChanged: (CameraPosition) -> Unit,
) {
    val cameraPosition = naverMap.cameraPosition
    onCameraPositionChanged(
        CameraPosition(
            center = Location(cameraPosition.target.latitude, cameraPosition.target.longitude),
            zoom = cameraPosition.zoom,
        ),
    )
}

private fun notifyBounds(
    naverMap: NaverMap,
    onBoundsChanged: (MapBounds) -> Unit,
) {
    val bounds = naverMap.contentBounds
    onBoundsChanged(
        MapBounds(
            minLat = bounds.southLatitude,
            maxLat = bounds.northLatitude,
            minLng = bounds.westLongitude,
            maxLng = bounds.eastLongitude,
        ),
    )
}

@Composable
private fun rememberMarkerBitmap(): Bitmap {
    val density = LocalDensity.current
    val painter = painterResource(Res.drawable.marker_ramen)
    return remember(painter, density) {
        RamenShopMarkerBitmapFactory().create(
            painter = painter,
            density = density,
            width = MarkerConfig.WIDTH,
            height = MarkerConfig.HEIGHT,
        )
    }
}

@Composable
private fun BindNaverMapLifecycle(
    mapView: MapView,
    lifecycle: Lifecycle,
) {
    DisposableEffect(mapView, lifecycle) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
