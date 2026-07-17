package com.peto.ramap.ui.account.contract

import com.peto.ramap.ui.base.State

data class AccountUiState(
    val isLoggedIn: Boolean = false,
    val accountLabel: String? = null,
    val isDeletingAccount: Boolean = false,
) : State
