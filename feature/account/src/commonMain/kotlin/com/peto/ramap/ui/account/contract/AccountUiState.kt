package com.peto.ramap.ui.account.contract

import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class AccountUiState(
    val isLoggedIn: Boolean = false,
    val accountLabel: String? = null,
    override val loadState: LoadState = LoadState(),
) : LoadableState<AccountUiState> {
    override fun withLoadingState(loadState: LoadState): AccountUiState = copy(loadState = loadState)
}
