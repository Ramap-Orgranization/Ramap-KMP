package com.peto.ramap.platform.permission

import androidx.compose.runtime.Composable

@Composable
expect fun rememberNotificationPermissionGenerator(onResult: (Boolean) -> Unit): NotificationPermissionGenerator
