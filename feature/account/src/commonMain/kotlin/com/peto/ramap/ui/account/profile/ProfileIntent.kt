package com.peto.ramap.ui.account.profile

import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.ui.base.Intent

sealed interface ProfileIntent : Intent {
    data object Retry : ProfileIntent

    data object Edit : ProfileIntent

    data class ChangeNickname(
        val value: String,
    ) : ProfileIntent

    data class ChangeBio(
        val value: String,
    ) : ProfileIntent

    data class ChangeInstagram(
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

    data object Discard : ProfileIntent

    data object KeepEditing : ProfileIntent

    data object Leave : ProfileIntent
}
