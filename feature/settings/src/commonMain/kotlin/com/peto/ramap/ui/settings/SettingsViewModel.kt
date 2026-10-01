package com.peto.ramap.ui.settings

import androidx.lifecycle.viewModelScope
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.settings.contract.SettingsIntent
import com.peto.ramap.ui.settings.contract.SettingsSideEffect
import com.peto.ramap.ui.settings.contract.SettingsUiState
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val loginRepository: LoginRepository,
) : BaseViewModel<SettingsUiState, SettingsIntent, SettingsSideEffect>(SettingsUiState()) {
    init {
        viewModelScope.launch {
            loginRepository.sessionState.collect { sessionState ->
                reduce { copy(isLoggedIn = sessionState == LoginSessionState.AUTHENTICATED) }
            }
        }
    }

    override suspend fun handleIntent(intent: SettingsIntent) = Unit
}
