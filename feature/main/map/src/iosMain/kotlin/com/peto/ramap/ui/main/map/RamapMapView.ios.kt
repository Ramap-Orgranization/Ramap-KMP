@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.main.map

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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import com.peto.ramap.ui.main.map.component.LocationButton
import com.peto.ramap.ui.main.map.config.CurrentLocationConfig
import com.peto.ramap.ui.main.map.model.CameraPosition
import com.peto.ramap.ui.main.map.model.CurrentLocationRequestState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
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
    var currentLocationRequestState by remember {
        mutableStateOf(CurrentLocationRequestState.Idle)
    }
    val controller =
        remember {
            IosNaverMapController(
                onMapMoveStarted = onMapMoveStarted,
                onBoundsChanged = onBoundsChanged,
                onCameraPositionChanged = onCameraPositionChanged,
                onShopClick = onShopClick,
                onMyLocationChanged = onMyLocationChanged,
                onCurrentLocationFocused = {
                    currentLocationRequestState = currentLocationRequestState.finish()
                },
            )
        }
    val permission =
        rememberLocationPermissionGenerator { status ->
            when (status) {
                PermissionStatus.Granted -> {
                    if (shouldBootstrapInitialLocationFocus) {
                        controller.bootstrapInitialLocationFocus()
                    }
                    if (currentLocationRequestState.isLoading) {
                        controller.requestCurrentLocation()
                    }
                }
                PermissionStatus.Blocked -> {
                    currentLocationRequestState = currentLocationRequestState.finish()
                    controller.cancelCurrentLocationRequest()
                    onLocationPermissionBlocked()
                }
                PermissionStatus.Denied -> {
                    currentLocationRequestState = currentLocationRequestState.finish()
                    controller.cancelCurrentLocationRequest()
                }
            }
        }

    Box(modifier = modifier.onSizeChanged { controller.viewportHeight = it.height }) {
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = { controller.view },
            update = {
                controller.restoreCameraPosition(cameraPosition)
                controller.updateShops(shops)
                controller.updateInitialLocationFocus(
                    location = initialFocusLocation,
                )
                controller.updatePlaceFocus(
                    location = placeFocusLocation,
                    requestKey = placeFocusRequestKey,
                )
                controller.updateFocus(
                    shops = focusShops,
                    focusNearestToCurrentLocation = focusNearestToCurrentLocation,
                    focusRequestKey = focusRequestKey,
                    selectedShopId = selectedShopId,
                )
                if (initialFocusLocation != null) {
                    onInitialFocusConsumed()
                }
            },
            properties =
                UIKitInteropProperties(
                    interactionMode = UIKitInteropInteractionMode.NonCooperative,
                    isNativeAccessibilityEnabled = false,
                ),
        )

        LocationButton(
            isLoading = currentLocationRequestState.isLoading,
            onClick = {
                if (currentLocationRequestState.isLoading) return@LocationButton
                currentLocationRequestState = currentLocationRequestState.start()
                if (permission.hasPermission()) {
                    controller.requestCurrentLocation()
                } else {
                    permission.requestPermission()
                }
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 16.dp),
        )
    }

    LaunchedEffect(Unit) {
        permission.requestPermission()
    }

    LaunchedEffect(currentLocationRequestState) {
        if (!currentLocationRequestState.isLoading) return@LaunchedEffect
        delay(CurrentLocationConfig.REQUEST_TIMEOUT_MILLIS)
        if (!currentLocationRequestState.isLoading) return@LaunchedEffect

        currentLocationRequestState = currentLocationRequestState.timeout()
        controller.cancelCurrentLocationRequest()
        onCurrentLocationTimeout()
    }

    DisposableEffect(controller) {
        onDispose(controller::dispose)
    }
}
