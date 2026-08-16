package com.peto.ramap.ui.notification

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.common.event.EventNotificationToggled
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.notification.contract.NotificationSettingsIntent
import com.peto.ramap.ui.notification.contract.NotificationSettingsIntent.OnEventNotificationsEnabledChanged
import com.peto.ramap.ui.notification.contract.NotificationSettingsIntent.OnNotificationSettingsRetried
import com.peto.ramap.ui.notification.contract.NotificationSettingsLoadKey
import com.peto.ramap.ui.notification.contract.NotificationSettingsSideEffect
import com.peto.ramap.ui.notification.contract.NotificationSettingsUiState
import com.peto.ramap.ui.task.TaskPolicy
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.personalization_update_failure_message

class NotificationSettingsViewModel(
    private val notificationRepository: NotificationSettingsRepository,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel<NotificationSettingsUiState, NotificationSettingsIntent, NotificationSettingsSideEffect>(
        initialState = NotificationSettingsUiState(),
    ) {
    init {
        fetchSettings()
    }

    override suspend fun handleIntent(intent: NotificationSettingsIntent) {
        when (intent) {
            OnNotificationSettingsRetried -> fetchSettings()
            is OnEventNotificationsEnabledChanged -> updateEnabled(intent.enabled)
        }
    }

    private fun fetchSettings() {
        launchResultTask(
            taskKey = FETCH_SETTINGS_TASK_KEY,
            loadKey = NotificationSettingsLoadKey.FETCH,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(showError = false) },
            request = notificationRepository::fetchEventNotificationsEnabled,
            onSuccess = { enabled ->
                reduce { copy(areEnabled = enabled) }
            },
            onError = { reduce { copy(showError = true) } },
        )
    }

    private fun updateEnabled(enabled: Boolean) {
        val previous = currentState.areEnabled
        analyticsTracker.logEvent(
            EventNotificationToggled(
                eventId = null,
                enabled = enabled,
                source = AnalyticsSource.NOTIFICATION_SETTINGS,
            ),
        )
        launchResultTask(
            taskKey = UPDATE_SETTINGS_TASK_KEY,
            loadKey = NotificationSettingsLoadKey.UPDATE,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(areEnabled = enabled) },
            request = { notificationRepository.updateEventNotificationsEnabled(enabled) },
            onError = {
                reduce { copy(areEnabled = previous) }
                trySideEffect(
                    NotificationSettingsSideEffect.ShowToast(
                        ToastData(
                            Res.string.personalization_update_failure_message,
                            ToastType.ERROR,
                        ),
                    ),
                )
            },
        )
    }

    companion object {
        private const val FETCH_SETTINGS_TASK_KEY = "fetch-notification-settings"
        private const val UPDATE_SETTINGS_TASK_KEY = "update-notification-settings"
    }
}
