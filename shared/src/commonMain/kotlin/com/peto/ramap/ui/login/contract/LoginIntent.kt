package com.peto.ramap.ui.login.contract

import com.peto.ramap.core.base.Intent
import com.peto.ramap.domain.model.LoginType

sealed interface LoginIntent : Intent {
    data class ClickLogin(
        val type: LoginType,
    ) : LoginIntent
}
