package com.peto.ramap.ui.account.profile

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.profile.ProfileInstagram
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.ui.base.BaseViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_image_rejected
import ramap.shared.generated.resources.profile_save_failed
import ramap.shared.generated.resources.profile_saved

class MyProfileViewModel(
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
                reduce { ProfileUiState(userId = userId, loading = userId != null, draftGeneration = ++nextDraftGeneration) }
                if (userId != null) fetch()
            }
        }
    }

    override suspend fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Retry -> fetch()
            ProfileIntent.Edit -> beginEdit()
            is ProfileIntent.ChangeNickname -> if (currentState.editing && !currentState.saving) reduce { copy(nickname = intent.value, nicknameTouched = true) }
            is ProfileIntent.ChangeBio -> if (currentState.editing && !currentState.saving) reduce { copy(bio = intent.value) }
            is ProfileIntent.ChangeInstagram -> if (currentState.editing && !currentState.saving) reduce { copy(instagram = intent.value) }
            is ProfileIntent.PickImage -> pickImage(intent)
            ProfileIntent.RejectImage -> trySideEffect(ProfileSideEffect.Toast(Res.string.profile_image_rejected))
            ProfileIntent.RemovePhoto -> if (!currentState.saving) reduce { copy(image = null, removePhoto = profile?.avatarUrl != null) }
            ProfileIntent.Save -> save()
            ProfileIntent.Back -> back()
            ProfileIntent.Discard, ProfileIntent.Leave -> endEdit()
            ProfileIntent.KeepEditing -> reduce { copy(confirmDiscard = false) }
        }
    }

    private fun fetch() {
        if (currentState.userId == null) return
        val generation = sessionGeneration
        launchTask(FETCH, onStart = { copy(loading = true, failed = false) }, onFinish = { copy(loading = false) }) {
            when (val result = repository.fetchMyProfile()) {
                is RamapResult.Success ->
                    if (generation == sessionGeneration && result.data.userId == currentState.userId) {
                        reduce { copy(profile = result.data, email = loginRepository.currentUserEmail()) }
                    }
                is RamapResult.Error -> if (generation == sessionGeneration) reduce { copy(failed = true) }
            }
        }
    }

    private fun beginEdit() {
        if (currentState.saving || currentState.editing) return
        val profile = currentState.profile ?: return
        reduce { copy(editing = true, nickname = profile.nickname, bio = profile.bio, instagram = profile.instagramUsername, image = null, removePhoto = false, nicknameTouched = false, draftGeneration = ++nextDraftGeneration) }
    }

    private fun pickImage(intent: ProfileIntent.PickImage) {
        if (!currentState.editing || currentState.saving || intent.generation != currentState.draftGeneration) return
        if (!intent.image.isValid()) {
            trySideEffect(ProfileSideEffect.Toast(Res.string.profile_image_rejected))
            return
        }
        reduce { copy(image = intent.image, removePhoto = false) }
    }

    private fun save() {
        val draft = currentState
        if (!draft.canSave) return
        val instagramUsername = ProfileInstagram.normalize(draft.instagram) ?: return
        val generation = sessionGeneration
        launchTask(SAVE, onStart = { copy(saving = true) }, onFinish = { copy(saving = false) }) {
            val result = repository.updateMyProfile(draft.nickname.trim(), draft.image, draft.removePhoto, bio = draft.bio.trim(), instagramUsername = instagramUsername)
            if (generation != sessionGeneration || draft.draftGeneration != currentState.draftGeneration) return@launchTask
            when (result) {
                is RamapResult.Success ->
                    if (result.data.userId == currentState.userId) {
                        reduce { copy(profile = result.data, editing = false, image = null, removePhoto = false, confirmDiscard = false, draftGeneration = ++nextDraftGeneration) }
                        trySideEffect(ProfileSideEffect.Toast(Res.string.profile_saved))
                    }
                is RamapResult.Error -> trySideEffect(ProfileSideEffect.Toast(Res.string.profile_save_failed))
            }
        }
    }

    private fun back() {
        when {
            !currentState.editing -> trySideEffect(ProfileSideEffect.NavigateBack)
            currentState.saving -> Unit
            currentState.changed -> reduce { copy(confirmDiscard = true) }
            else -> endEdit()
        }
    }

    private fun endEdit() {
        nextDraftGeneration++
        cancelTask(SAVE)
        reduce { copy(editing = false, nickname = "", bio = "", instagram = "", image = null, removePhoto = false, nicknameTouched = false, confirmDiscard = false, draftGeneration = nextDraftGeneration) }
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
    }
}
