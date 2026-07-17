package com.peto.ramap.ui.main.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.ui.main.my.contract.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val loginRepository: LoginRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch { observeSessionState() }
    }

    private suspend fun observeSessionState() {
        loginRepository.sessionState.collectLatest { sessionState ->
            mutableUiState.value =
                SettingsUiState(
                    isLoggedIn = sessionState == LoginSessionState.AUTHENTICATED,
                )
        }
    }
}
