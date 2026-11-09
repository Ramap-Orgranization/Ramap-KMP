package com.peto.ramap.platform.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat

@Composable
actual fun rememberNotificationPermissionGenerator(onResult: (NotificationPermissionOutcome) -> Unit): NotificationPermissionGenerator {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val outcome =
                when {
                    !granted -> NotificationPermissionOutcome.NotGranted
                    !NotificationManagerCompat.from(context).areNotificationsEnabled() -> NotificationPermissionOutcome.DisabledInSettings
                    else -> NotificationPermissionOutcome.Granted
                }
            currentOnResult(outcome)
        }

    return remember(context, permissionLauncher) {
        AndroidNotificationPermissionGenerator(
            context = context,
            permissionLauncher = permissionLauncher,
            onResult = { currentOnResult(it) },
        )
    }
}
