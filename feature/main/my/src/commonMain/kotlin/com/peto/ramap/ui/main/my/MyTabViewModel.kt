package com.peto.ramap.ui.main.my

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.profile.ProfileVisibility
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.repository.CommunityRepository
import com.peto.ramap.domain.repository.FollowRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import com.peto.ramap.ui.main.my.contract.MyTabLoadKey
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect
import com.peto.ramap.ui.main.my.contract.MyTabUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.profile_save_failed
import ramap.shared.generated.resources.review_action_failed
import ramap.shared.generated.resources.review_blocked_users_empty

class MyTabViewModel(
    private val repository: ProfileRepository,
    private val communityRepository: CommunityRepository,
    private val personalizationStore: PersonalizationStore,
    private val reviewRepository: ReviewRepository,
    private val followRepository: FollowRepository,
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
                cancelTask(FOLLOW_REQUESTS)
                cancelTask(UNBLOCK)
                reduce { MyTabUiState(userId = userId, sessionResolved = userId == null) }
                if (userId != null) refresh()
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
            MyTabIntent.ReturnedToScreen -> {
                refreshReviewCount()
                refreshBlockedUserCount()
                refreshPendingFollowRequests()
            }
            MyTabIntent.ReturnedFromProfileEdit -> {
                refreshProfile()
                refreshPendingFollowRequests()
            }
            MyTabIntent.ReturnedFromReviews -> {
                refreshReviewCount()
                refreshPendingFollowRequests()
            }
            MyTabIntent.ReturnedFromBlockedProfile -> {
                refreshBlockedUserCount()
                refreshPendingFollowRequests()
            }
            MyTabIntent.ReturnedFromFollows -> refreshPendingFollowRequests()
            is MyTabIntent.SaveVisibility ->
                saveVisibility(
                    ProfileVisibility(
                        isPublic = intent.isPublic,
                        followersCanReadReviews = currentState.profile?.followersCanReadReviews ?: true,
                        followersCanReadSavedShops = currentState.profile?.followersCanReadSavedShops ?: true,
                    ),
                )
            is MyTabIntent.SaveProfileVisibility -> saveVisibility(intent.visibility)
            MyTabIntent.OpenBlockedUsers -> openBlockedUsers()
            MyTabIntent.DismissBlockedUsers -> {
                if (!currentState.unblocking) {
                    reduce { copy(blockedUsers = emptyList(), pendingUnblockUser = null) }
                }
            }
            is MyTabIntent.RequestUnblock -> requestUnblock(intent.userId)
            MyTabIntent.DismissUnblock -> {
                if (!currentState.unblocking) reduce { copy(pendingUnblockUser = null) }
            }
            MyTabIntent.ConfirmUnblock -> unblockUser()
        }
    }

    private fun requestUnblock(userId: String) {
        if (currentState.unblocking) return
        val target = currentState.blockedUsers.firstOrNull { it.profile.userId == userId } ?: return
        reduce { copy(pendingUnblockUser = target.profile) }
    }

    private fun unblockUser() {
        val currentUserId = currentState.userId ?: return
        val target = currentState.pendingUnblockUser ?: return
        if (currentState.unblocking || currentState.blockedUsers.none { it.profile.userId == target.userId }) return
        launchResultTask(
            taskKey = UNBLOCK,
            loadKey = MyTabLoadKey.Unblock,
            policy = TaskPolicy.IgnoreNew,
            request = { communityRepository.unblockUser(target.userId) },
            onSuccess = { completeUnblock(currentUserId, target.userId) },
            onError = { showUnblockFailure(currentUserId) },
        )
    }

    private suspend fun completeUnblock(
        currentUserId: String,
        targetUserId: String,
    ) {
        if (currentState.userId != currentUserId) return
        reduce {
            val remainingUsers = blockedUsers.filterNot { it.profile.userId == targetUserId }
            copy(
                blockedUsers = remainingUsers,
                blockedUserCount = remainingUsers.size,
                pendingUnblockUser = null,
            )
        }
        if (currentState.blockedUsers.isEmpty()) {
            postSideEffect(MyTabSideEffect.CloseBlockedUsersDialog)
        }
    }

    private suspend fun showUnblockFailure(currentUserId: String) {
        if (currentState.userId != currentUserId) return
        postSideEffect(
            MyTabSideEffect.ShowToast(
                ToastData(
                    message = Res.string.review_action_failed,
                    type = ToastType.ERROR,
                ),
            ),
        )
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

    private fun saveVisibility(visibility: ProfileVisibility) {
        val userId = currentState.userId ?: return
        val profile = currentState.profile ?: return
        if (profile.userId != userId || currentState.savingVisibility) return
        if (profile.isPublic == visibility.isPublic &&
            profile.followersCanReadReviews == visibility.followersCanReadReviews &&
            profile.followersCanReadSavedShops == visibility.followersCanReadSavedShops
        ) {
            trySideEffect(MyTabSideEffect.VisibilitySaved)
            return
        }
        launchResultTask(
            taskKey = VISIBILITY,
            loadKey = MyTabLoadKey.Visibility,
            request = { repository.updateProfileVisibility(visibility) },
            onSuccess = { updatedProfile ->
                if (currentState.userId == userId && updatedProfile.userId == userId) {
                    reduce { copy(profile = updatedProfile) }
                    postSideEffect(MyTabSideEffect.VisibilitySaved)
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
        refreshReviewCount()
        refreshBlockedUserCount()
        refreshPendingFollowRequests()
        refreshProfile()
    }

    private fun refreshPendingFollowRequests() {
        val userId = currentState.userId ?: return
        launchResultTask(
            taskKey = FOLLOW_REQUESTS,
            request = followRepository::fetchCounts,
            onSuccess = { counts ->
                if (currentState.userId == userId) reduce { copy(hasPendingFollowRequests = counts.requests > 0L) }
            },
        )
    }

    private fun refreshProfile() {
        val userId = currentState.userId ?: return
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
        private const val FOLLOW_REQUESTS = "my-tab-follow-requests"
        private const val UNBLOCK = "my-tab-unblock-user"
    }
}
