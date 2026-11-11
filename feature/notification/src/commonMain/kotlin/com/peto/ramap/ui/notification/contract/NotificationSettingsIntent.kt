package com.peto.ramap.ui.notification.contract

import com.peto.ramap.ui.base.Intent

sealed interface NotificationSettingsIntent : Intent {
    data object OnNotificationSettingsRetried : NotificationSettingsIntent

    data object OnReviewNotificationSettingsRetried : NotificationSettingsIntent

    data class OnReviewNotificationsEnabledChanged(
        val accountId: String,
        val enabled: Boolean,
    ) : NotificationSettingsIntent

    data class OnEventNotificationsEnabledChanged(
        val enabled: Boolean,
    ) : NotificationSettingsIntent
}
