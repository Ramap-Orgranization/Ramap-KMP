package com.peto.ramap.platform.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.CoreLocation.CLLocationManager

@Composable
internal actual fun rememberLocationPermissionGenerator(onResult: (PermissionStatus) -> Unit): LocationPermissionGenerator {
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        IosLocationPermissionGenerator(
            locationManager = CLLocationManager(),
            onResult = { result -> currentOnResult(result) },
        )
    }
}
