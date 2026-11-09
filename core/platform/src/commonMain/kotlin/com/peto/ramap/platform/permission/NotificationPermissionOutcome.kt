package com.peto.ramap.platform.permission

enum class NotificationPermissionOutcome(
    val isGranted: Boolean,
) {
    Granted(true),
    AlreadyGranted(true),
    EnabledWithoutRuntimeRequest(true),
    NotGranted(false),
    DisabledInSettings(false),
    Failed(false),
}
