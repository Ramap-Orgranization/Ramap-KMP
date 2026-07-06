package com.peto.ramap.platform.permission

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberLocationPermissionGenerator(onResult: (PermissionStatus) -> Unit): LocationPermissionGenerator
