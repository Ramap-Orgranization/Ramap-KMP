package com.peto.ramap.ui.profile.edit

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileDraft
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.loading.LoadState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_bio_daily_change_limit_reached
import ramap.shared.generated.resources.profile_image_rejected
import ramap.shared.generated.resources.profile_nickname_check_rate_limited
import ramap.shared.generated.resources.profile_nickname_daily_change_limit_reached
import ramap.shared.generated.resources.profile_save_failed
import ramap.shared.generated.resources.profile_save_rate_limited
import ramap.shared.generated.resources.profile_saved

class ProfileEditViewModel(
    private val repository: ProfileRepository,
    private val loginRepository: LoginRepository,
) : BaseViewModel<ProfileUiState, ProfileIntent, ProfileSideEffect>(ProfileUiState()) {
    private var sessionGeneration = 0L
    private var nextDraftGeneration = 0L

    private fun showToast(
        message: StringResource,
        type: ToastType = ToastType.DEFAULT,
    ) {
        trySideEffect(ProfileSideEffect.ShowToast(message, type))
    }

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
            ProfileIntent.RejectImage -> showToast(Res.string.profile_image_rejected, ToastType.ERROR)
            ProfileIntent.RemovePhoto -> if (!currentState.saving) reduce { copy(image = null, removePhoto = profile?.avatarUrl != null) }
            ProfileIntent.Save -> save()
            ProfileIntent.Back -> back()
            ProfileIntent.Leave -> endEdit()
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
                checkedNickname = null,
                draftGeneration = ++nextDraftGeneration,
            )
        }
    }

    private fun pickImage(intent: ProfileIntent.PickImage) {
        if (!currentState.editing || currentState.saving || intent.generation != currentState.draftGeneration) return
        if (!intent.image.isValid()) {
            showToast(Res.string.profile_image_rejected, ToastType.ERROR)
            return
        }
        reduce { copy(image = intent.image, removePhoto = false) }
    }

    private fun changeNickname(value: String) {
        if (!currentState.editing || currentState.saving) return
        if (value.trim() != currentState.nickname.trim()) cancelTask(CHECK_NICKNAME)
        reduce {
            if (value.trim() == nickname.trim()) {
                copy(nickname = value, nicknameTouched = true)
            } else {
                copy(
                    nickname = value,
                    nicknameTouched = true,
                    nicknameAvailable = null,
                    nicknameCheckFailed = false,
                    checkedNickname = null,
                )
            }
        }
    }

    private fun checkNickname() {
        val draft = currentState
        if (!draft.canCheckNickname) return
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
                is RamapResult.Success -> reduce { copy(nicknameAvailable = result.data, checkedNickname = nickname) }
                is RamapResult.Error -> {
                    if (isRateLimit(result.error)) {
                        showToast(Res.string.profile_nickname_check_rate_limited, ToastType.ERROR)
                    } else {
                        reduce { copy(nicknameCheckFailed = true) }
                    }
                }
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
                        reduce { copy(profile = result.data, editing = false, image = null, removePhoto = false, draftGeneration = ++nextDraftGeneration) }
                        showToast(Res.string.profile_saved, ToastType.SUCCESS)
                        trySideEffect(ProfileSideEffect.NavigateBack)
                    }

                is RamapResult.Error -> handleSaveFailure(draft, result.error)
            }
        }
    }

    private suspend fun handleSaveFailure(
        draft: ProfileUiState,
        error: RamapError,
    ) {
        if (isProfileSaveRateLimit(error)) {
            showToast(Res.string.profile_save_rate_limited, ToastType.ERROR)
            return
        }
        if (isProfileDailyChangeLimit(error)) {
            showToast(dailyChangeLimitMessage(error), ToastType.ERROR)
            return
        }
        if (isNicknameTaken(error)) {
            reduce {
                copy(
                    nicknameAvailable = false,
                    nicknameCheckFailed = false,
                    checkedNickname = draft.nickname.trim(),
                )
            }
            return
        }
        if (draft.nicknameChanged) {
            val availability = repository.isNicknameAvailable(draft.nickname.trim())
            if (draft.draftGeneration != currentState.draftGeneration || draft.userId != currentState.userId) return
            if (availability is RamapResult.Success && !availability.data) {
                reduce { copy(nicknameAvailable = false) }
                return
            }
        }
        showToast(Res.string.profile_save_failed, ToastType.ERROR)
    }

    private fun isProfileDailyChangeLimit(error: RamapError): Boolean {
        if (error !is RamapError.Http || error.status != HTTP_TOO_MANY_REQUESTS_STATUS) return false
        return error.serverMessage in setOf(PROFILE_NICKNAME_DAILY_LIMIT, PROFILE_BIO_DAILY_LIMIT)
    }

    private fun dailyChangeLimitMessage(error: RamapError): StringResource =
        when ((error as RamapError.Http).serverMessage) {
            PROFILE_NICKNAME_DAILY_LIMIT -> Res.string.profile_nickname_daily_change_limit_reached
            PROFILE_BIO_DAILY_LIMIT -> Res.string.profile_bio_daily_change_limit_reached
            else -> error("Unsupported profile daily change limit")
        }

    private fun isNicknameTaken(error: RamapError): Boolean =
        error is RamapError.Http &&
            error.status == HTTP_CONFLICT_STATUS &&
            error.serverMessage == PROFILE_NICKNAME_TAKEN

    private fun isRateLimit(error: RamapError): Boolean = error is RamapError.Http && error.status == HTTP_TOO_MANY_REQUESTS_STATUS

    private fun isProfileSaveRateLimit(error: RamapError): Boolean {
        if (error !is RamapError.Http || error.status != HTTP_TOO_MANY_REQUESTS_STATUS) return false
        return error.serverMessage == PROFILE_SAVE_RATE_LIMIT
    }

    private fun back() {
        when {
            !currentState.editing -> trySideEffect(ProfileSideEffect.NavigateBack)
            currentState.saving -> Unit
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
        reduce { copy(editing = false, nickname = "", bio = "", image = null, removePhoto = false, nicknameTouched = false, nicknameAvailable = null, nicknameCheckFailed = false, checkedNickname = null, draftGeneration = nextDraftGeneration) }
    }

    override fun handleError(throwable: Throwable) {
        super.handleError(throwable)
        if (currentState.profile == null) {
            reduce { copy(failed = true) }
        } else {
            showToast(Res.string.profile_save_failed, ToastType.ERROR)
        }
    }

    companion object {
        private const val FETCH = "profile-fetch"
        private const val SAVE = "profile-save"
        private const val CHECK_NICKNAME = "profile-check-nickname"
        private const val HTTP_TOO_MANY_REQUESTS_STATUS = 429
        private const val HTTP_CONFLICT_STATUS = 409
        private const val PROFILE_NICKNAME_TAKEN = "PROFILE_NICKNAME_TAKEN"
        private const val PROFILE_NICKNAME_DAILY_LIMIT = "PROFILE_NICKNAME_DAILY_LIMIT"
        private const val PROFILE_BIO_DAILY_LIMIT = "PROFILE_BIO_DAILY_LIMIT"
        private const val PROFILE_SAVE_RATE_LIMIT = "PROFILE_SAVE_RATE_LIMIT"
    }
}
