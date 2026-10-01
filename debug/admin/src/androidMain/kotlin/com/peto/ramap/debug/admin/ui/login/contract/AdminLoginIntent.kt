package com.peto.ramap.debug.admin.ui.login.contract

import com.peto.ramap.ui.base.Intent

internal sealed interface AdminLoginIntent : Intent {
    data object OnAdminLoginClicked : AdminLoginIntent
}
