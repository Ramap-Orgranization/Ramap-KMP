package com.peto.ramap.ui.main.my

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewCommunityRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import com.peto.ramap.ui.main.my.contract.MyTabLoadKey
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect
import com.peto.ramap.ui.main.my.contract.MyTabUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.profile_save_failed
import ramap.shared.generated.resources.review_blocked_users_empty

class MyTabViewModel(
    private val repository: ProfileRepository,
    private val communityRepository: ReviewCommunityRepository,
    private val personalizationStore: ShopPersonalizationStore,
    private val reviewRepository: ReviewRepository,
) : BaseViewModel<MyTabUiState, MyTabIntent, MyTabSideEffect>(MyTabUiState()) {
    private var requestGeneration = 0L

    init {
        viewModelScope.launch {
            repository.sessionUserIds.distinctUntilChanged().collect { userId ->
                requestGeneration++
                cancelTask(FETCH)
                cancelTask(VISIBILITY)
                cancelTask(BLOCKED_USERS)
                cancelTask(REVIEW_COUNT)
                cancelTask(BLOCKED_USER_COUNT)
                reduce { MyTabUiState(userId = userId, sessionResolved = userId == null) }
                if (userId != null) refresh()
            }
        }
        viewModelScope.launch {
            reviewRepository.observeChanges().collect {
                if (currentState.userId != null) refreshReviewCount()
            }
        }
        viewModelScope.launch {
            repository.observeProfileUpdates().collect { profile ->
                if (currentState.userId == profile.userId) reduce { copy(profile = profile) }
            }
        }
        viewModelScope.launch {
            communityRepository.observeChanges().collect {
                if (currentState.userId != null) refreshBlockedUserCount()
            }
        }
        viewModelScope.launch {
            personalizationStore.state.collect { state ->
                val personalization = (state as? PersonalizationBootstrapState.Success)?.value
                reduce {
                    copy(
                        bookmarkedCount = personalization?.bookmarkedShopIds?.size,
                        notificationCount = personalization?.notificationShopIds?.size,
                        hiddenCount = personalization?.hiddenShopIds?.size,
                    )
                }
            }
        }
    }

    override suspend fun handleIntent(intent: MyTabIntent) {
        when (intent) {
            MyTabIntent.Refresh -> refresh()
            is MyTabIntent.SaveVisibility -> saveVisibility(intent.isPublic)
            MyTabIntent.OpenBlockedUsers -> openBlockedUsers()
            MyTabIntent.DismissBlockedUsers -> reduce { copy(blockedUsers = emptyList()) }
        }
    }

    private fun openBlockedUsers() {
        val userId = currentState.userId ?: return
        launchResultTask(
            taskKey = BLOCKED_USERS,
            loadKey = MyTabLoadKey.BlockedUsers,
            request = communityRepository::fetchBlockedUsers,
            onSuccess = { users ->
                if (currentState.userId == userId) {
                    reduce { copy(blockedUserCount = users.size) }
                    if (users.isEmpty()) {
                        postSideEffect(
                            MyTabSideEffect.ShowToast(
                                ToastData(
                                    message = Res.string.review_blocked_users_empty,
                                    type = ToastType.DEFAULT,
                                ),
                            ),
                        )
                    } else {
                        reduce { copy(blockedUsers = users) }
                        postSideEffect(MyTabSideEffect.OpenBlockedUsersDialog)
                    }
                }
            },
            onError = {
                if (currentState.userId == userId) {
                    reduce { copy(blockedUsers = emptyList()) }
                    postSideEffect(
                        MyTabSideEffect.ShowToast(
                            ToastData(
                                message = Res.string.data_load_failure_message,
                                type = ToastType.ERROR,
                            ),
                        ),
                    )
                }
            },
        )
    }

    private fun saveVisibility(isPublic: Boolean) {
        val userId = currentState.userId ?: return
        val profile = currentState.profile ?: return
        if (profile.userId != userId || currentState.savingVisibility || profile.isPublic == isPublic) return
        launchResultTask(
            taskKey = VISIBILITY,
            loadKey = MyTabLoadKey.Visibility,
            request = { repository.updateProfileVisibility(isPublic) },
            onSuccess = { updatedProfile ->
                if (currentState.userId == userId && updatedProfile.userId == userId) {
                    reduce { copy(profile = updatedProfile) }
                }
            },
            onError = {
                if (currentState.userId == userId) {
                    postSideEffect(
                        MyTabSideEffect.ShowToast(
                            ToastData(
                                message = Res.string.profile_save_failed,
                                type = ToastType.DEFAULT,
                            ),
                        ),
                    )
                }
            },
        )
    }

    private fun refresh() {
        val userId = currentState.userId ?: return
        refreshReviewCount()
        refreshBlockedUserCount()
        val generation = ++requestGeneration
        launchResultTask(
            taskKey = FETCH,
            loadKey = MyTabLoadKey.Fetch,
            onStart = { copy(failed = false, sessionResolved = true) },
            request = repository::fetchMyProfile,
            onSuccess = { profile ->
                if (generation == requestGeneration && currentState.userId == userId) {
                    reduce { if (profile.userId == userId) copy(profile = profile) else copy(failed = true) }
                }
            },
            onError = {
                if (generation == requestGeneration && currentState.userId == userId) reduce { copy(failed = true) }
            },
        )
    }

    private fun refreshReviewCount() {
        val userId = currentState.userId ?: return
        launchResultTask(
            taskKey = REVIEW_COUNT,
            loadKey = MyTabLoadKey.ReviewCount,
            request = { reviewRepository.fetchMyReviews(0, MyReviewVisibility.ALL) },
            onSuccess = { page ->
                if (currentState.userId == userId) reduce { copy(reviewCount = page.totalCount) }
            },
        )
    }

    private fun refreshBlockedUserCount() {
        val userId = currentState.userId ?: return
        launchResultTask(
            taskKey = BLOCKED_USER_COUNT,
            loadKey = MyTabLoadKey.BlockedUserCount,
            request = communityRepository::fetchBlockedUsers,
            onSuccess = { users ->
                if (currentState.userId == userId) reduce { copy(blockedUserCount = users.size) }
            },
        )
    }

    companion object {
        private const val FETCH = "my-tab-profile-fetch"
        private const val VISIBILITY = "my-tab-profile-visibility"
        private const val BLOCKED_USERS = "my-tab-blocked-users"
        private const val REVIEW_COUNT = "my-tab-review-count"
        private const val BLOCKED_USER_COUNT = "my-tab-blocked-user-count"
    }
}
