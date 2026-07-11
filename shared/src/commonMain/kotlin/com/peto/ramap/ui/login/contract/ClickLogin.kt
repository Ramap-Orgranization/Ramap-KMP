package com.peto.ramap.ui.login.contract

import com.peto.ramap.domain.model.LoginType

data class ClickLogin(
    val type: LoginType,
) : LoginIntent
