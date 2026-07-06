package com.peto.ramap.platform.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

internal val LOCATION_PERMISSIONS =
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

@Composable
internal actual fun rememberLocationPermissionGenerator(onResult: (PermissionStatus) -> Unit): LocationPermissionGenerator {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            when {
                isLocationGranted(permissions) -> currentOnResult(PermissionStatus.Granted)
                isLocationPermissionBlocked(context) -> currentOnResult(PermissionStatus.Blocked)
                else -> currentOnResult(PermissionStatus.Denied)
            }
        }

    return remember(context, permissionLauncher) {
        AndroidLocationPermissionGenerator(
            context = context,
            permissionLauncher = permissionLauncher,
            onResult = { result -> currentOnResult(result) },
        )
    }
}

private fun isLocationGranted(permissions: Map<String, Boolean>): Boolean =
    permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

private fun isLocationPermissionBlocked(context: Context): Boolean {
    val activity = findActivity(context) ?: return false

    return !hasLocationPermission(context) &&
        LOCATION_PERMISSIONS.none { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
}

internal fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

private fun findActivity(context: Context): Activity? =
    when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }
