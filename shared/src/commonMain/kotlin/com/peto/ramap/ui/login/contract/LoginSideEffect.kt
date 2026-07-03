package com.peto.ramap.ui.login.contract

import com.peto.ramap.core.base.SideEffect

sealed interface LoginSideEffect : SideEffect {
    data object LoginSuccess : LoginSideEffect
}
