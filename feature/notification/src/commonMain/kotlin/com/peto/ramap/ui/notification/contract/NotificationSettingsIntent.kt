package com.peto.ramap.ui.notification.contract

import com.peto.ramap.ui.base.Intent

sealed interface NotificationSettingsIntent : Intent {
    data object OnNotificationSettingsRetried : NotificationSettingsIntent

    data class OnEventNotificationsEnabledChanged(
        val enabled: Boolean,
    ) : NotificationSettingsIntent
}
