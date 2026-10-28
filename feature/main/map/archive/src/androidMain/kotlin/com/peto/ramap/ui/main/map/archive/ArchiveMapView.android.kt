package com.peto.ramap.ui.main.map.archive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
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
import com.naver.maps.map.util.FusedLocationSource
import com.peto.ramap.designsystem.button.LocationButton
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.platform.permission.findActivity
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import com.peto.ramap.ui.main.map.archive.model.ArchiveCurrentLocationConfig
import com.peto.ramap.ui.main.map.archive.model.ArchiveMapInteractionConfig
import com.peto.ramap.ui.main.map.archive.model.ArchiveMapRenderModel
import com.peto.ramap.ui.main.map.archive.model.CameraPosition
import com.peto.ramap.ui.main.map.archive.model.CurrentLocationRequestState
import com.peto.ramap.ui.main.map.archive.model.InitialMapRenderState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal actual fun ArchiveMapView(
    request: ArchiveMapRenderModel,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentRequest by rememberUpdatedState(request)
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    var viewportHeight by remember { mutableStateOf(0) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var shouldMoveToCurrentLocation by remember { mutableStateOf(false) }
    var locationRequestState by remember { mutableStateOf(CurrentLocationRequestState.Idle) }
    val initialRenderState = remember { InitialMapRenderState() }
    val cameraController = remember { ArchiveCameraController() }
    val locationSource =
        remember(context) {
            findActivity(context)?.let { FusedLocationSource(it, LOCATION_PERMISSION_REQUEST_CODE) }
        }
    val clusterRenderer =
        remember(request.clusteringOptions) {
            ArchiveShopClusterRenderer(
                clusteringOptions = request.clusteringOptions,
                onShopClick = { currentRequest.onShopClick(it) },
                onClusterClick = { shops, x, y -> currentRequest.onClusterClick(shops, IntOffset(x, y)) },
            )
        }
    val moveToCurrentLocation = {
        val map = naverMap
        val location = currentLocation
        if (map != null && location != null) {
            map.locationTrackingMode = LocationTrackingMode.NoFollow
            map.moveCamera(
                CameraUpdate.scrollAndZoomTo(
                    LatLng(location.lat, location.lng),
                    ArchiveCurrentLocationConfig.zoomForCurrentLocation(map.cameraPosition.zoom),
                ),
            )
            shouldMoveToCurrentLocation = false
            locationRequestState = locationRequestState.finish()
        }
    }
    val permission =
        rememberLocationPermissionGenerator { status ->
            when (status) {
                PermissionStatus.Granted -> {
                    if (shouldMoveToCurrentLocation) {
                        naverMap?.locationTrackingMode = LocationTrackingMode.NoFollow
                        moveToCurrentLocation()
                    }
                }

                PermissionStatus.Blocked -> {
                    shouldMoveToCurrentLocation = false
                    locationRequestState = locationRequestState.finish()
                    currentRequest.onLocationPermissionBlocked()
                }

                PermissionStatus.Denied -> {
                    shouldMoveToCurrentLocation = false
                    locationRequestState = locationRequestState.finish()
                }
            }
        }

    BindArchiveMapLifecycle(mapView = mapView, lifecycle = lifecycle)
    LaunchedEffect(naverMap, request.shops, request.markerImages, clusterRenderer) {
        val map = naverMap ?: return@LaunchedEffect
        clusterRenderer.render(map, request.shops, request.markerImages)
    }
    LaunchedEffect(naverMap, request.shouldFocusShops, request.focusShops, request.selectedShopId, viewportHeight) {
        val map = naverMap ?: return@LaunchedEffect
        if (viewportHeight == 0) return@LaunchedEffect
        val focusShops = request.focusShops.takeIf { request.shouldFocusShops }
        map.setContentPadding(
            0,
            0,
            0,
            if (request.selectedShopId != null && focusShops?.size == 1) {
                (viewportHeight * ArchiveMapInteractionConfig.SELECTED_MARKER_BOTTOM_PADDING_RATIO).toInt()
            } else {
                0
            },
        )
        val didMoveCamera = focusShops?.let { cameraController.focusShops(map, it) } ?: false
        initialRenderState.cameraConfigured()
        if (!didMoveCamera && initialRenderState.onFrameRendered(map.isFullyRendered, map.isRenderingStable)) {
            currentRequest.onMapReady()
        }
        if (didMoveCamera) currentRequest.onFocusConsumed()
    }
    LaunchedEffect(locationRequestState) {
        if (!locationRequestState.isLoading) return@LaunchedEffect
        delay(ArchiveCurrentLocationConfig.REQUEST_TIMEOUT_MILLIS.milliseconds)
        if (!locationRequestState.isLoading) return@LaunchedEffect
        shouldMoveToCurrentLocation = false
        locationRequestState = locationRequestState.timeout()
        currentRequest.onCurrentLocationTimeout()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onSizeChanged { viewportHeight = it.height },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.getMapAsync { map ->
                    map.minZoom = ArchiveMapInteractionConfig.MAX_ZOOM_OUT_LEVEL.toDouble()
                    map.maxZoom = ArchiveMapInteractionConfig.MAP_MAX_ZOOM_LEVEL.toDouble()
                    map.locationSource = locationSource
                    map.uiSettings.isCompassEnabled = false
                    map.uiSettings.isLocationButtonEnabled = false
                    map.uiSettings.isZoomControlEnabled = false
                    map.addOnMapRenderedListener { fully, stable ->
                        if (initialRenderState.onFrameRendered(fully, stable)) currentRequest.onMapReady()
                    }
                    map.addOnLocationChangeListener { location ->
                        currentLocation = Location(location.latitude, location.longitude)
                        if (shouldMoveToCurrentLocation) moveToCurrentLocation()
                    }
                    var isCameraMoving = false
                    map.addOnCameraChangeListener { _, _ ->
                        if (!isCameraMoving) {
                            isCameraMoving = true
                            currentRequest.onMapMoveStarted()
                        }
                    }
                    map.addOnCameraIdleListener {
                        isCameraMoving = false
                        val position = map.cameraPosition
                        currentRequest.onCameraChanged(
                            CameraPosition(
                                center = Location(position.target.latitude, position.target.longitude),
                                zoom = position.zoom,
                            ),
                        )
                    }
                    currentRequest.cameraPosition?.let { position ->
                        map.moveCamera(CameraUpdate.scrollAndZoomTo(LatLng(position.center.lat, position.center.lng), position.zoom))
                    }
                    naverMap = map
                }
                mapView
            },
        )
        LocationButton(
            isLoading = locationRequestState.isLoading,
            onClick = {
                if (locationRequestState.isLoading) return@LocationButton
                locationRequestState = locationRequestState.start()
                shouldMoveToCurrentLocation = true
                if (permission.hasPermission()) {
                    naverMap?.locationTrackingMode = LocationTrackingMode.NoFollow
                    moveToCurrentLocation()
                } else {
                    permission.requestPermission()
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 16.dp),
        )
    }
    DisposableEffect(clusterRenderer) {
        onDispose { clusterRenderer.dispose() }
    }
}

@Composable
private fun BindArchiveMapLifecycle(
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
