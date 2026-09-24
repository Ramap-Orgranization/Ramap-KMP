package com.peto.ramap.ui.main.my.profile

import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileBio
import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.domain.model.profile.ProfileInstagram
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
    val instagram: String = "",
    val image: ProfileImage? = null,
    val removePhoto: Boolean = false,
    val nicknameTouched: Boolean = false,
    val nicknameAvailable: Boolean? = null,
    val nicknameCheckFailed: Boolean = false,
    val confirmDiscard: Boolean = false,
    val draftGeneration: Long = 0,
    override val loadState: LoadState = LoadState.loading(ProfileLoadKey.Fetch),
) : LoadableState<ProfileUiState> {
    val loading: Boolean get() = loadState.isLoading(ProfileLoadKey.Fetch)
    val checkingNickname: Boolean get() = loadState.isLoading(ProfileLoadKey.CheckNickname)
    val saving: Boolean get() = loadState.isLoading(ProfileLoadKey.Save)

    val changed: Boolean get() = nickname.trim() != profile?.nickname || bio.trim() != profile?.bio || ProfileInstagram.normalize(instagram) != profile?.instagramUsername || image != null || removePhoto
    val nicknameInvalid: Boolean get() = !ProfileNickname.isValid(nickname.trim())
    val nicknameChanged: Boolean get() = nickname.trim() != profile?.nickname
    val bioInvalid: Boolean get() = !ProfileBio.isValid(bio.trim())
    val instagramInvalid: Boolean get() = ProfileInstagram.normalize(instagram) == null
    val canSave: Boolean get() = editing && changed && !nicknameInvalid && (!nicknameChanged || nicknameAvailable == true) && !checkingNickname && !bioInvalid && !instagramInvalid && !saving

    override fun withLoadingState(loadState: LoadState): ProfileUiState = copy(loadState = loadState)
}
