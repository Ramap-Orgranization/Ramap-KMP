package com.peto.ramap.ui.settings.contract

import com.peto.ramap.ui.base.State

data class SettingsUiState(
    val isLoggedIn: Boolean = false,
) : State
