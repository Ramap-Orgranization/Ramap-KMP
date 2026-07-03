package com.peto.ramap.ui.login.contract

import com.peto.ramap.core.base.State
import com.peto.ramap.domain.model.LoginType
import com.peto.ramap.domain.model.supportedLoginTypes

data class LoginUiState(
    val loginTypes: List<LoginType> = supportedLoginTypes(),
) : State
