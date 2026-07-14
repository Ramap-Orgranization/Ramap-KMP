package com.peto.ramap.ui.main.my.contract

import com.peto.ramap.core.base.Intent

sealed interface MyTabIntent : Intent {
    data object OnKakaoLoginClick : MyTabIntent

    data object OnLogoutClick : MyTabIntent

    data object OnAccountDeleteClick : MyTabIntent

    data object OnAccountDeleteDismiss : MyTabIntent

    data object OnAccountDeleteConfirm : MyTabIntent

    data class OnPlaceUrlChanged(
        val value: String,
    ) : MyTabIntent

    data object OnPlaceReportSubmit : MyTabIntent

    data object OnCurrentLocationReportSubmit : MyTabIntent

    data object OnCurrentAddressRefresh : MyTabIntent

    data object OnHiddenShopsClick : MyTabIntent

    data object OnLoginGuideDismiss : MyTabIntent
}
