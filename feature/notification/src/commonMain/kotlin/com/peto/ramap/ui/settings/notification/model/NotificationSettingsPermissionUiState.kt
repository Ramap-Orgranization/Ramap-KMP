package com.peto.ramap.ui.settings.notification.model

data class NotificationSettingsPermissionUiState(
    val isGranted: Boolean = false,
    val isEnablePending: Boolean = false,
) {
    val shouldEnableServerNotifications: Boolean
        get() = isGranted && isEnablePending

    fun isEnabled(isServerEnabled: Boolean): Boolean = isServerEnabled && isGranted

    fun onEnableRequestResult(isGranted: Boolean) =
        copy(
            isGranted = isGranted,
            isEnablePending = !isGranted,
        )

    fun onResume(isGranted: Boolean) = copy(isGranted = isGranted)

    fun consumePendingEnable() = copy(isEnablePending = false)

    fun onDisabled() = copy(isEnablePending = false)
}
