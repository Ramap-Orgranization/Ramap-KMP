package com.peto.ramap.ui.main.my.contract

import com.peto.ramap.domain.model.profile.ProfileVisibility
import com.peto.ramap.ui.base.Intent

sealed interface MyTabIntent : Intent {
    data object Refresh : MyTabIntent

    data object ReturnedToScreen : MyTabIntent

    data object ReturnedFromProfileEdit : MyTabIntent

    data object ReturnedFromReviews : MyTabIntent

    data object ReturnedFromBlockedProfile : MyTabIntent

    data object ReturnedFromFollows : MyTabIntent

    data class SaveProfileVisibility(
        val visibility: ProfileVisibility,
    ) : MyTabIntent

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
