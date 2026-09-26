package com.peto.ramap.ui.profile.edit

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileDraft
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.loading.LoadState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_image_rejected
import ramap.shared.generated.resources.profile_save_failed
import ramap.shared.generated.resources.profile_saved

class ProfileEditViewModel(
    private val repository: ProfileRepository,
    private val loginRepository: LoginRepository,
) : BaseViewModel<ProfileUiState, ProfileIntent, ProfileSideEffect>(ProfileUiState()) {
    private var sessionGeneration = 0L
    private var nextDraftGeneration = 0L

    init {
        viewModelScope.launch {
            repository.sessionUserIds.distinctUntilChanged().collect { userId ->
                sessionGeneration++
                cancelTask(FETCH)
                cancelTask(SAVE)
                cancelTask(CHECK_NICKNAME)
                reduce { ProfileUiState(userId = userId, draftGeneration = ++nextDraftGeneration, loadState = LoadState()) }
                if (userId != null) fetch()
            }
        }
    }

    override suspend fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Retry -> fetch()
            is ProfileIntent.ChangeNickname -> changeNickname(intent.value)
            ProfileIntent.CheckNickname -> checkNickname()
            is ProfileIntent.ChangeBio -> if (currentState.editing && !currentState.saving) reduce { copy(bio = intent.value) }
            is ProfileIntent.PickImage -> pickImage(intent)
            ProfileIntent.RejectImage -> trySideEffect(ProfileSideEffect.Toast(Res.string.profile_image_rejected))
            ProfileIntent.RemovePhoto -> if (!currentState.saving) reduce { copy(image = null, removePhoto = profile?.avatarUrl != null) }
            ProfileIntent.Save -> save()
            ProfileIntent.Back -> back()
            ProfileIntent.Discard -> discardAndNavigateBack()
            ProfileIntent.Leave -> endEdit()
            ProfileIntent.KeepEditing -> reduce { copy(confirmDiscard = false) }
        }
    }

    private fun fetch() {
        if (currentState.userId == null) return
        val generation = sessionGeneration
        launchTask(FETCH, loadKey = ProfileLoadKey.Fetch, onStart = { copy(failed = false) }) {
            when (val result = repository.fetchMyProfile()) {
                is RamapResult.Success ->
                    if (generation == sessionGeneration && result.data.userId == currentState.userId) {
                        beginEdit(result.data)
                    }
                is RamapResult.Error -> if (generation == sessionGeneration) reduce { copy(failed = true) }
            }
        }
    }

    private fun beginEdit(profile: AccountProfile) {
        reduce {
            copy(
                profile = profile,
                email = loginRepository.currentUserEmail(),
                editing = true,
                nickname = profile.nickname,
                bio = profile.bio,
                image = null,
                removePhoto = false,
                nicknameTouched = false,
                nicknameAvailable = null,
                nicknameCheckFailed = false,
                draftGeneration = ++nextDraftGeneration,
            )
        }
    }

    private fun pickImage(intent: ProfileIntent.PickImage) {
        if (!currentState.editing || currentState.saving || intent.generation != currentState.draftGeneration) return
        if (!intent.image.isValid()) {
            trySideEffect(ProfileSideEffect.Toast(Res.string.profile_image_rejected))
            return
        }
        reduce { copy(image = intent.image, removePhoto = false) }
    }

    private fun changeNickname(value: String) {
        if (!currentState.editing || currentState.saving) return
        cancelTask(CHECK_NICKNAME)
        reduce {
            copy(
                nickname = value,
                nicknameTouched = true,
                nicknameAvailable = null,
                nicknameCheckFailed = false,
            )
        }
    }

    private fun checkNickname() {
        val draft = currentState
        if (!draft.editing || draft.saving || draft.checkingNickname || draft.nicknameInvalid || !draft.nicknameChanged) return
        val nickname = draft.nickname.trim()
        val generation = sessionGeneration
        launchTask(
            taskKey = CHECK_NICKNAME,
            loadKey = ProfileLoadKey.CheckNickname,
            onStart = { copy(nicknameCheckFailed = false, nicknameAvailable = null) },
        ) {
            val result = repository.isNicknameAvailable(nickname)
            if (generation != sessionGeneration || draft.draftGeneration != currentState.draftGeneration || nickname != currentState.nickname.trim()) return@launchTask
            when (result) {
                is RamapResult.Success -> reduce { copy(nicknameAvailable = result.data) }
                is RamapResult.Error -> reduce { copy(nicknameCheckFailed = true) }
            }
        }
    }

    private fun save() {
        val draft = currentState
        if (!draft.canSave) return
        val generation = sessionGeneration
        launchTask(SAVE, loadKey = ProfileLoadKey.Save) {
            val result =
                repository.updateMyProfile(
                    ProfileDraft.of(
                        nickname = draft.nickname.trim(),
                        image = draft.image,
                        removePhoto = draft.removePhoto,
                        bio = draft.bio.trim(),
                    ),
                )
            if (generation != sessionGeneration || draft.draftGeneration != currentState.draftGeneration) return@launchTask
            when (result) {
                is RamapResult.Success ->
                    if (result.data.userId == currentState.userId) {
                        reduce { copy(profile = result.data, editing = false, image = null, removePhoto = false, confirmDiscard = false, draftGeneration = ++nextDraftGeneration) }
                        trySideEffect(ProfileSideEffect.Toast(Res.string.profile_saved))
                        trySideEffect(ProfileSideEffect.NavigateBack)
                    }
                is RamapResult.Error -> handleSaveFailure(draft)
            }
        }
    }

    private suspend fun handleSaveFailure(draft: ProfileUiState) {
        if (draft.nicknameChanged) {
            val availability = repository.isNicknameAvailable(draft.nickname.trim())
            if (draft.draftGeneration != currentState.draftGeneration || draft.userId != currentState.userId) return
            if (availability is RamapResult.Success && !availability.data) {
                reduce { copy(nicknameAvailable = false) }
                return
            }
        }
        trySideEffect(ProfileSideEffect.Toast(Res.string.profile_save_failed))
    }

    private fun back() {
        when {
            !currentState.editing -> trySideEffect(ProfileSideEffect.NavigateBack)
            currentState.saving -> Unit
            currentState.changed -> reduce { copy(confirmDiscard = true) }
            else -> discardAndNavigateBack()
        }
    }

    private fun discardAndNavigateBack() {
        endEdit()
        trySideEffect(ProfileSideEffect.NavigateBack)
    }

    private fun endEdit() {
        nextDraftGeneration++
        cancelTask(SAVE)
        cancelTask(CHECK_NICKNAME)
        reduce { copy(editing = false, nickname = "", bio = "", image = null, removePhoto = false, nicknameTouched = false, nicknameAvailable = null, nicknameCheckFailed = false, confirmDiscard = false, draftGeneration = nextDraftGeneration) }
    }

    override fun handleError(throwable: Throwable) {
        super.handleError(throwable)
        if (currentState.profile == null) {
            reduce { copy(failed = true) }
        } else {
            trySideEffect(ProfileSideEffect.Toast(Res.string.profile_save_failed))
        }
    }

    companion object {
        private const val FETCH = "profile-fetch"
        private const val SAVE = "profile-save"
        private const val CHECK_NICKNAME = "profile-check-nickname"
    }
}
