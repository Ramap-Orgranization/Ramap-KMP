package com.peto.ramap.ui.account.contract

import com.peto.ramap.ui.base.Intent

sealed interface AccountIntent : Intent {
    data object OnKakaoLoginClick : AccountIntent

    data object OnAppleLoginClick : AccountIntent

    data object OnLogoutClick : AccountIntent

    data object OnAccountDeleteConfirm : AccountIntent
}
