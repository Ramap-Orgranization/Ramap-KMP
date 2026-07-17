package com.peto.ramap.ui.notification

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.notification.contract.NotificationSettingsIntent
import com.peto.ramap.ui.notification.contract.NotificationSettingsIntent.OnEventNotificationsEnabledChanged
import com.peto.ramap.ui.notification.contract.NotificationSettingsIntent.OnNotificationSettingsRetried
import com.peto.ramap.ui.notification.contract.NotificationSettingsSideEffect
import com.peto.ramap.ui.notification.contract.NotificationSettingsUiState
import kotlinx.coroutines.launch

class NotificationSettingsViewModel(
    private val notificationRepository: NotificationSettingsRepository,
) : BaseViewModel<NotificationSettingsUiState, NotificationSettingsIntent, NotificationSettingsSideEffect>(
        initialState = NotificationSettingsUiState(),
    ) {
    init {
        viewModelScope.launch { loadSettings() }
    }

    override suspend fun handleIntent(intent: NotificationSettingsIntent) {
        when (intent) {
            OnNotificationSettingsRetried -> loadSettings()
            is OnEventNotificationsEnabledChanged -> updateEnabled(intent.enabled)
        }
    }

    private suspend fun loadSettings() {
        reduce { copy(loadState = LoadState.Loading) }
        handleResult(
            result = notificationRepository.fetchEventNotificationsEnabled(),
            onSuccess = { enabled ->
                reduce { copy(loadState = LoadState.Content(Unit), areEnabled = enabled) }
            },
            onError = { reduce { copy(loadState = LoadState.Error) } },
        )
    }

    private suspend fun updateEnabled(enabled: Boolean) {
        val previous = currentState.areEnabled
        reduce { copy(areEnabled = enabled) }
        if (notificationRepository.updateEventNotificationsEnabled(enabled) is RamapResult.Error) {
            reduce { copy(areEnabled = previous) }
        }
    }
}
