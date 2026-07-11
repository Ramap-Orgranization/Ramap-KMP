package com.peto.ramap.ui.map

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.util.FusedLocationSource
import com.peto.ramap.core.config.DefaultMapConfig
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.core.config.MarkerConfig
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.platform.permission.findActivity
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.marker_ramen

@Composable
actual fun RamapMapView(
    shops: RamenShops,
    focusShops: List<RamenShop>,
    focusNearestToCurrentLocation: Boolean,
    focusRequestKey: Long,
    selectedShopId: String?,
    onBoundsChanged: (MapBounds) -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    var viewportHeight by remember { mutableStateOf(0) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val locationSource =
        remember(context) {
            findActivity(context)?.let { activity ->
                FusedLocationSource(activity, LOCATION_PERMISSION_REQUEST_CODE)
            }
        }
    val cameraController = remember { NaverCameraController() }
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
                    naverMap?.locationTrackingMode = LocationTrackingMode.Follow
                }

                PermissionStatus.Blocked -> onLocationPermissionBlocked()
                PermissionStatus.Denied -> Unit
            }
        }

    BindNaverMapLifecycle(mapView, lifecycle)

    LaunchedEffect(naverMap, shops, clusterRenderer) {
        val map = naverMap ?: return@LaunchedEffect
        clusterRenderer.render(map, shops.values.toList())
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

    LaunchedEffect(naverMap) {
        if (naverMap != null) locationPermissionGenerator.requestPermission()
    }

    AndroidView(
        modifier = modifier.onSizeChanged { viewportHeight = it.height },
        factory = {
            mapView.getMapAsync(
                OnMapReadyCallback { map ->
                    map.minZoom = MapInteractionConfig.MAX_ZOOM_OUT_LEVEL.toDouble()
                    map.locationSource = locationSource
                    map.uiSettings.isLocationButtonEnabled = true
                    map.addOnLocationChangeListener { location ->
                        Location(location.latitude, location.longitude).let { current ->
                            currentLocation = current
                            onMyLocationChanged(current)
                        }
                    }
                    map.addOnCameraIdleListener { notifyBounds(map, onBoundsChanged) }
                    map.moveCamera(
                        CameraUpdate.scrollAndZoomTo(
                            LatLng(DefaultMapConfig.LATITUDE, DefaultMapConfig.LONGITUDE),
                            DefaultMapConfig.ZOOM_LEVEL.toDouble(),
                        ),
                    )
                    naverMap = map
                    mapView.post { notifyBounds(map, onBoundsChanged) }
                },
            )
            mapView
        },
    )

    DisposableEffect(clusterRenderer) {
        onDispose { clusterRenderer.dispose() }
    }
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
