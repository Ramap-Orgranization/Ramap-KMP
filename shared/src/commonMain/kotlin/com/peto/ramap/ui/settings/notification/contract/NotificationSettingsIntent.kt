package com.peto.ramap.ui.settings.notification.contract

import com.peto.ramap.core.base.Intent

sealed interface NotificationSettingsIntent : Intent {
    data object OnResumed : NotificationSettingsIntent

    data class OnEnabledChanged(
        val enabled: Boolean,
    ) : NotificationSettingsIntent

    data class OnShopRemoved(
        val shopId: String,
    ) : NotificationSettingsIntent

    data class OnEventOverrideRemoved(
        val eventId: String,
    ) : NotificationSettingsIntent

    data class OnRemovalRequested(
        val target: NotificationRemovalTarget,
    ) : NotificationSettingsIntent

    data object OnRemovalDismissed : NotificationSettingsIntent

    data object OnRemovalConfirmed : NotificationSettingsIntent
}
