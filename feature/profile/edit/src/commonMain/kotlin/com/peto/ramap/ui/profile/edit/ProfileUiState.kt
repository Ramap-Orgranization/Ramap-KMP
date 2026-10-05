package com.peto.ramap.ui.profile.edit

import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileBio
import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.domain.model.profile.ProfileNickname
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class ProfileUiState(
    val userId: String? = null,
    val profile: AccountProfile? = null,
    val email: String? = null,
    val failed: Boolean = false,
    val editing: Boolean = false,
    val nickname: String = "",
    val bio: String = "",
    val image: ProfileImage? = null,
    val removePhoto: Boolean = false,
    val nicknameTouched: Boolean = false,
    val nicknameAvailable: Boolean? = null,
    val nicknameCheckFailed: Boolean = false,
    val checkedNickname: String? = null,
    override val loadState: LoadState = LoadState.loading(ProfileLoadKey.Fetch),
) : LoadableState<ProfileUiState> {
    val loading: Boolean get() = loadState.isLoading(ProfileLoadKey.Fetch)
    val checkingNickname: Boolean get() = loadState.isLoading(ProfileLoadKey.CheckNickname)
    val saving: Boolean get() = loadState.isLoading(ProfileLoadKey.Save)

    val changed: Boolean get() = nickname.trim() != profile?.nickname || bio.trim() != profile.bio || image != null || removePhoto
    val nicknameInvalid: Boolean get() = !ProfileNickname.isValid(nickname.trim())
    val nicknameContainsProfanity: Boolean get() = ProfileNickname.containsProfanity(nickname.trim())
    val nicknameChanged: Boolean get() = nickname.trim() != profile?.nickname
    val canCheckNickname: Boolean get() = editing && nicknameChanged && !nicknameInvalid && checkedNickname != nickname.trim() && !checkingNickname && !saving
    val showNicknameStatus: Boolean get() = !nicknameInvalid && nicknameChanged && (nicknameAvailable != null || nicknameCheckFailed)
    val bioInvalid: Boolean get() = !ProfileBio.isValid(bio.trim())
    val bioChanged: Boolean get() = bio.trim() != profile?.bio
    val canSave: Boolean get() = editing && changed && !nicknameInvalid && (!nicknameChanged || nicknameAvailable == true) && !checkingNickname && !bioInvalid && !saving

    override fun withLoadingState(loadState: LoadState): ProfileUiState = copy(loadState = loadState)
}
