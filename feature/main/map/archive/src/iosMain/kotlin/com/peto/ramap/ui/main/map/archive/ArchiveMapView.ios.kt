@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.peto.ramap.designsystem.button.LocationButton
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import com.peto.ramap.ui.main.map.archive.model.ArchiveCurrentLocationConfig
import com.peto.ramap.ui.main.map.archive.model.ArchiveMapRenderModel
import com.peto.ramap.ui.main.map.archive.model.CurrentLocationRequestState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun ArchiveMapView(
    request: ArchiveMapRenderModel,
    modifier: Modifier,
) {
    val currentRequest by rememberUpdatedState(request)
    val density by rememberUpdatedState(LocalDensity.current)
    var viewportHeight by remember { mutableStateOf(0) }
    var locationRequestState by remember { mutableStateOf(CurrentLocationRequestState.Idle) }
    val controller =
        remember(request.clusteringOptions) {
            ArchiveIosMapController(
                clusteringOptions = request.clusteringOptions,
                onMapMoveStarted = { currentRequest.onMapMoveStarted() },
                onCameraPositionChanged = { currentRequest.onCameraChanged(it) },
                onShopClick = { currentRequest.onShopClick(it) },
                onClusterClick = { shops, x, y ->
                    currentRequest.onClusterClick(
                        shops,
                        IntOffset(x = (x * density.density).roundToInt(), y = (y * density.density).roundToInt()),
                    )
                },
                onCurrentLocationFocused = { locationRequestState = locationRequestState.finish() },
                onMapReady = { currentRequest.onMapReady() },
            )
        }
    val permission =
        rememberLocationPermissionGenerator { status ->
            when (status) {
                PermissionStatus.Granted -> if (locationRequestState.isLoading) controller.requestCurrentLocation()
                PermissionStatus.Blocked -> {
                    locationRequestState = locationRequestState.finish()
                    controller.cancelCurrentLocationRequest()
                    currentRequest.onLocationPermissionBlocked()
                }
                PermissionStatus.Denied -> {
                    locationRequestState = locationRequestState.finish()
                    controller.cancelCurrentLocationRequest()
                }
            }
        }
    Box(modifier = modifier.fillMaxSize().onSizeChanged { viewportHeight = it.height }) {
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = { controller.view },
            update = {
                controller.updateViewportHeight(viewportHeight, density.density)
                controller.updateShops(request.shops, request.markerImages)
                if (viewportHeight > 0) {
                    controller.restoreCameraPosition(request.cameraPosition)
                    val didMoveCamera =
                        controller.updateFocus(
                            shops = if (request.shouldFocusShops) request.focusShops else RamenShops(emptyMap()),
                            selectedShopId = request.selectedShopId,
                        )
                    if (didMoveCamera) currentRequest.onFocusConsumed()
                    controller.initialCameraConfigured()
                }
            },
            properties =
                UIKitInteropProperties(
                    interactionMode = UIKitInteropInteractionMode.NonCooperative,
                    isNativeAccessibilityEnabled = true,
                ),
        )
        LocationButton(
            isLoading = locationRequestState.isLoading,
            onClick = {
                if (locationRequestState.isLoading) return@LocationButton
                locationRequestState = locationRequestState.start()
                if (permission.hasPermission()) controller.requestCurrentLocation() else permission.requestPermission()
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 16.dp),
        )
    }
    LaunchedEffect(locationRequestState) {
        if (!locationRequestState.isLoading) return@LaunchedEffect
        delay(ArchiveCurrentLocationConfig.REQUEST_TIMEOUT_MILLIS)
        if (!locationRequestState.isLoading) return@LaunchedEffect
        locationRequestState = locationRequestState.timeout()
        controller.cancelCurrentLocationRequest()
        currentRequest.onCurrentLocationTimeout()
    }
    DisposableEffect(controller) {
        onDispose(controller::dispose)
    }
}
