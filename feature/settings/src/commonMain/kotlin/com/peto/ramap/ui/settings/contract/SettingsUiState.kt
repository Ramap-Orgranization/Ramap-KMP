package com.peto.ramap.ui.settings

import com.peto.ramap.ui.base.State

data class SettingsUiState(
    val isLoggedIn: Boolean = false,
) : State
