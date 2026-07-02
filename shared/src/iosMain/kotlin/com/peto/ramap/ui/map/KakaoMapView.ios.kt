package com.peto.ramap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShops
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun KakaoMapView(
    shops: RamenShops,
    focusShops: List<RamenShop>,
    bounds: MapBounds,
    clusterBounds: MapBounds,
    myLocationRequestKey: Int,
    locationSettingsRequestKey: Int,
    onBoundsChanged: (MapBounds) -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    modifier: Modifier,
) {
    val mapController =
        remember {
            IosKakaoMapController(
                onBoundsChanged = onBoundsChanged,
                onShopClick = onShopClick,
                onLocationPermissionBlocked = onLocationPermissionBlocked,
            )
        }
    var viewportWidth by remember { mutableStateOf(0) }
    var viewportHeight by remember { mutableStateOf(0) }

    UIKitView(
        modifier =
            modifier.onSizeChanged { size ->
                viewportWidth = size.width
                viewportHeight = size.height
            },
        factory = {
            mapController.view
        },
        update = {
            mapController.updateShops(
                shops = shops,
                bounds = bounds,
                clusterBounds = clusterBounds,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
            )
            mapController.updateFocusShops(focusShops)
        },
        properties =
            UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = false,
            ),
    )

    LaunchedEffect(myLocationRequestKey) {
        if (myLocationRequestKey > 0) {
            mapController.moveToMyLocation()
        }
    }

    LaunchedEffect(locationSettingsRequestKey) {
        if (locationSettingsRequestKey > 0) {
            mapController.openAppSettings()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapController.dispose()
        }
    }
}
