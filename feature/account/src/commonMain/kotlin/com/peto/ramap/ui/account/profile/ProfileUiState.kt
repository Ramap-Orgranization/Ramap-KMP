package com.peto.ramap.ui.account.profile

import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileBio
import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.domain.model.profile.ProfileInstagram
import com.peto.ramap.domain.model.profile.ProfileNickname
import com.peto.ramap.ui.base.State

data class ProfileUiState(
    val userId: String? = null,
    val profile: AccountProfile? = null,
    val email: String? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
    val editing: Boolean = false,
    val nickname: String = "",
    val bio: String = "",
    val instagram: String = "",
    val image: ProfileImage? = null,
    val removePhoto: Boolean = false,
    val nicknameTouched: Boolean = false,
    val saving: Boolean = false,
    val confirmDiscard: Boolean = false,
    val draftGeneration: Long = 0,
) : State {
    val changed: Boolean get() = nickname.trim() != profile?.nickname || bio.trim() != profile?.bio || ProfileInstagram.normalize(instagram) != profile?.instagramUsername || image != null || removePhoto
    val nicknameInvalid: Boolean get() = !ProfileNickname.isValid(nickname.trim())
    val bioInvalid: Boolean get() = !ProfileBio.isValid(bio.trim())
    val instagramInvalid: Boolean get() = ProfileInstagram.normalize(instagram) == null
    val canSave: Boolean get() = editing && changed && !nicknameInvalid && !bioInvalid && !instagramInvalid && !saving
}
