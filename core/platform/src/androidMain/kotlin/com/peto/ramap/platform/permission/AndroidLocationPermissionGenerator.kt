package com.peto.ramap.platform.permission

import android.content.Context
import androidx.activity.result.ActivityResultLauncher

internal class AndroidLocationPermissionGenerator(
    private val context: Context,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>,
    private val onResult: (PermissionStatus) -> Unit,
) : LocationPermissionGenerator {
    override fun hasPermission(): Boolean = hasLocationPermission(context)

    override fun requestPermission() {
        if (hasPermission()) {
            onResult(PermissionStatus.Granted)
            return
        }

        permissionLauncher.launch(LOCATION_PERMISSIONS)
    }
}
