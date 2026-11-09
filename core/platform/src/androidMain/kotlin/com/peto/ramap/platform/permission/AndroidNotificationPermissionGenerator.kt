package com.peto.ramap.platform.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

internal class AndroidNotificationPermissionGenerator(
    private val context: Context,
    private val permissionLauncher: ActivityResultLauncher<String>,
    private val onResult: (Boolean) -> Unit,
) : NotificationPermissionGenerator {
    override fun requestPermission() {
        val hasRuntimePermission =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (hasRuntimePermission) {
            onResult(NotificationManagerCompat.from(context).areNotificationsEnabled())
            return
        }

        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
