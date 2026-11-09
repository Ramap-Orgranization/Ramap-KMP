package com.peto.ramap.platform.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberNotificationPermissionGenerator(onResult: (NotificationPermissionOutcome) -> Unit): NotificationPermissionGenerator {
    val currentOnResult by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()

    return remember(scope) {
        IosNotificationPermissionGenerator(
            scope = scope,
            onResult = { currentOnResult(it) },
        )
    }
}
