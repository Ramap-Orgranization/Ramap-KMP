@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.main.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun RamapMapView(
    shops: RamenShops,
    focusShops: RamenShops,
    focusNearestToCurrentLocation: Boolean,
    focusRequestKey: Long,
    initialFocusLocation: Location?,
    initialFocusRequestKey: Long,
    shouldBootstrapInitialLocationFocus: Boolean,
    selectedShopId: String?,
    onMapMoveStarted: () -> Unit,
    onBoundsChanged: (MapBounds) -> Unit,
    onInitialFocusConsumed: () -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    modifier: Modifier,
) {
    val controller =
        remember {
            IosNaverMapController(
                onMapMoveStarted = onMapMoveStarted,
                onBoundsChanged = onBoundsChanged,
                onShopClick = onShopClick,
                onMyLocationChanged = onMyLocationChanged,
            )
        }
    val permission =
        rememberLocationPermissionGenerator { status ->
            when (status) {
                PermissionStatus.Granted -> {
                    if (shouldBootstrapInitialLocationFocus) {
                        controller.bootstrapInitialLocationFocus()
                    }
                }
                PermissionStatus.Blocked -> onLocationPermissionBlocked()
                PermissionStatus.Denied -> Unit
            }
        }

    UIKitView(
        modifier = modifier.onSizeChanged { controller.viewportHeight = it.height },
        factory = { controller.view },
        update = {
            controller.updateShops(shops)
            controller.updateInitialLocationFocus(
                location = initialFocusLocation,
                requestKey = initialFocusRequestKey,
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

    LaunchedEffect(Unit) {
        permission.requestPermission()
    }

    DisposableEffect(controller) {
        onDispose(controller::dispose)
    }
}
