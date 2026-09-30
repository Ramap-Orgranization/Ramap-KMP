package com.peto.ramap.ui.profile.edit

import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.ui.base.Intent

sealed interface ProfileIntent : Intent {
    data object Retry : ProfileIntent

    data class ChangeNickname(
        val value: String,
    ) : ProfileIntent

    data object CheckNickname : ProfileIntent

    data class ChangeBio(
        val value: String,
    ) : ProfileIntent

    data class PickImage(
        val image: ProfileImage,
        val generation: Long,
    ) : ProfileIntent

    data object RejectImage : ProfileIntent

    data object RemovePhoto : ProfileIntent

    data object Save : ProfileIntent

    data object Back : ProfileIntent

    data object Leave : ProfileIntent
}
