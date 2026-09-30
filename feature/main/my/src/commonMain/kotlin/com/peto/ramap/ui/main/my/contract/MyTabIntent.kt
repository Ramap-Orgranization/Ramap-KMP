package com.peto.ramap.ui.main.my.contract

import com.peto.ramap.ui.base.Intent

sealed interface MyTabIntent : Intent {
    data object Refresh : MyTabIntent

    data class SaveVisibility(
        val isPublic: Boolean,
    ) : MyTabIntent

    data object OpenBlockedUsers : MyTabIntent

    data object DismissBlockedUsers : MyTabIntent

    data class RequestUnblock(
        val userId: String,
    ) : MyTabIntent

    data object DismissUnblock : MyTabIntent

    data object ConfirmUnblock : MyTabIntent
}
