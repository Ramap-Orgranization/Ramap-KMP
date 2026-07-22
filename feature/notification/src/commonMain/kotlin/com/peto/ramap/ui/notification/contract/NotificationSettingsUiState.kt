package com.peto.ramap.ui.notification.contract

import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class NotificationSettingsUiState(
    val areEnabled: Boolean = false,
    val showError: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<NotificationSettingsUiState> {
    override fun withLoadingState(loadState: LoadState): NotificationSettingsUiState = copy(loadState = loadState)
}
