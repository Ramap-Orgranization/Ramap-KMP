package com.peto.ramap.ui.notification.contract

import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.common.LoadState

data class NotificationSettingsUiState(
    val loadState: LoadState<Unit> = LoadState.Idle,
    val areEnabled: Boolean = false,
) : State
