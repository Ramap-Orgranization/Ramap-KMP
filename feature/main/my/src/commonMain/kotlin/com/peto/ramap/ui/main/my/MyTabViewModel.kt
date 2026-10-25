package com.peto.ramap.ui.main.my

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.profile.ProfileVisibility
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.repository.FollowRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.repository.UserModerationRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.store.ReviewChangeStore
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
import ramap.shared.generated.resources.review_blocked_users_empty
import ramap.shared.generated.resources.review_unblock_failed

class MyTabViewModel(
    private val repository: ProfileRepository,
    private val userModerationRepository: UserModerationRepository,
    private val personalizationStore: PersonalizationStore,
    private val reviewRepository: ReviewRepository,
    private val followRepository: FollowRepository,
    private val changes: ReviewChangeStore,
) : BaseViewModel<MyTabUiState, MyTabIntent, MyTabSideEffect>(MyTabUiState()) {
    private var requestGeneration = 0L
    private var sessionGeneration = 0L

    init {
        viewModelScope.launch {
            var epoch = changes.journal.value.epoch
            var revision = changes.journal.value.revision
            changes.journal.collect { journal ->
                if (epoch != journal.epoch) {
                    epoch = journal.epoch
                    revision = journal.revision
                }
                val hasGap = journal.hasGapAfter(revision)
                if (hasGap) refreshReviewCount()
                for (record in journal.changesAfter(revision)) {
                    val change = record.change as? ReviewChange.Written ?: continue
                    val isMyReview = change.userId == currentState.userId
                    if (isMyReview) reduce { copy(reviewCount = change.counts.totalCount) }
                }
                revision = journal.revision
            }
        }
        viewModelScope.launch {
            repository.sessionUserId.distinctUntilChanged().collect { userId ->
                requestGeneration++
                sessionGeneration++
                cancelTask(FETCH)
                cancelTask(VISIBILITY)
                cancelTask(BLOCKED_USERS)
                cancelTask(REVIEW_COUNT)
                cancelTask(BLOCKED_USER_COUNT)
                cancelTask(FOLLOW_REQUESTS)
                cancelTask(UNBLOCK)
                val isSessionResolved = userId == null
                reduce { MyTabUiState(userId = userId, sessionResolved = isSessionResolved) }
                val hasUserId = userId != null
                if (hasUserId) refresh()
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
                refreshBlockedUserCount()
                refreshPendingFollowRequests()
            }
            MyTabIntent.ReturnedFromProfileEdit -> {
                refreshProfile()
                refreshPendingFollowRequests()
            }
            MyTabIntent.ReturnedFromReviews -> {
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
                val isNotUnblocking = !currentState.unblocking
                if (isNotUnblocking) {
                    reduce { copy(blockedUsers = emptyList(), pendingUnblockUser = null) }
                }
            }
            is MyTabIntent.RequestUnblock -> requestUnblock(intent.userId)
            MyTabIntent.DismissUnblock -> {
                val isNotUnblocking = !currentState.unblocking
                if (isNotUnblocking) reduce { copy(pendingUnblockUser = null) }
            }
            MyTabIntent.ConfirmUnblock -> unblockUser()
        }
    }

    private fun requestUnblock(userId: String) {
        val isAlreadyUnblocking = currentState.unblocking
        if (isAlreadyUnblocking) return
        val target = currentState.blockedUsers.firstOrNull { it.profile.userId == userId } ?: return
        reduce { copy(pendingUnblockUser = target.profile) }
    }

    private fun unblockUser() {
        val currentUserId = currentState.userId ?: return
        val target = currentState.pendingUnblockUser ?: return
        val isBlockedTargetMissing = currentState.blockedUsers.none { it.profile.userId == target.userId }
        val cannotUnblock = currentState.unblocking || isBlockedTargetMissing
        if (cannotUnblock) return
        launchResultTask(
            taskKey = UNBLOCK,
            loadKey = MyTabLoadKey.Unblock,
            policy = TaskPolicy.IgnoreNew,
            request = { userModerationRepository.unblockUser(target.userId) },
            onSuccess = { completeUnblock(currentUserId, target.userId) },
            onError = { showUnblockFailure(currentUserId) },
        )
    }

    private suspend fun completeUnblock(
        currentUserId: String,
        targetUserId: String,
    ) {
        val isDifferentUser = currentState.userId != currentUserId
        if (isDifferentUser) return
        reduce {
            val remainingUsers = blockedUsers.filterNot { it.profile.userId == targetUserId }
            copy(
                blockedUsers = remainingUsers,
                blockedUserCount = remainingUsers.size,
                pendingUnblockUser = null,
            )
        }
        val isBlockedListEmpty = currentState.blockedUsers.isEmpty()
        if (isBlockedListEmpty) {
            postSideEffect(MyTabSideEffect.CloseBlockedUsersDialog)
        }
    }

    private suspend fun showUnblockFailure(currentUserId: String) {
        val isDifferentUser = currentState.userId != currentUserId
        if (isDifferentUser) return
        postSideEffect(
            MyTabSideEffect.ShowToast(
                ToastData(
                    message = Res.string.review_unblock_failed,
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
            request = userModerationRepository::fetchBlockedUsers,
            onSuccess = { users ->
                val isSameUser = currentState.userId == userId
                if (isSameUser) {
                    reduce { copy(blockedUserCount = users.size) }
                    val isUsersEmpty = users.isEmpty()
                    if (isUsersEmpty) {
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
                val isSameUser = currentState.userId == userId
                if (isSameUser) {
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
        val isProfileSaveNotAllowed = profile.userId != userId || currentState.savingVisibility
        if (isProfileSaveNotAllowed) return

        val isVisibilityUnchanged =
            profile.isPublic == visibility.isPublic &&
                profile.followersCanReadReviews == visibility.followersCanReadReviews &&
                profile.followersCanReadSavedShops == visibility.followersCanReadSavedShops
        if (isVisibilityUnchanged) {
            trySideEffect(MyTabSideEffect.VisibilitySaved)
            return
        }

        launchResultTask(
            taskKey = VISIBILITY,
            loadKey = MyTabLoadKey.Visibility,
            request = { repository.updateProfileVisibility(visibility) },
            onSuccess = { updatedProfile ->
                val isSameUser = currentState.userId == userId && updatedProfile.userId == userId
                if (isSameUser) {
                    reduce { copy(profile = updatedProfile) }
                    postSideEffect(MyTabSideEffect.VisibilitySaved)
                }
            },
            onError = {
                val isSameUser = currentState.userId == userId
                if (isSameUser) {
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
                val isSameUser = currentState.userId == userId
                if (isSameUser) reduce { copy(hasPendingFollowRequests = counts.requests > 0L) }
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
                val isCurrentGeneration = generation == requestGeneration && currentState.userId == userId
                if (isCurrentGeneration) {
                    reduce { if (profile.userId == userId) copy(profile = profile) else copy(failed = true) }
                }
            },
            onError = {
                val isCurrentGeneration = generation == requestGeneration && currentState.userId == userId
                if (isCurrentGeneration) reduce { copy(failed = true) }
            },
        )
    }

    private fun refreshReviewCount() {
        val userId = currentState.userId ?: return
        val generation = sessionGeneration
        val baseline = changes.journal.value
        launchResultTask(
            taskKey = REVIEW_COUNT,
            loadKey = MyTabLoadKey.ReviewCount,
            request = { reviewRepository.fetchMyReviews(0, MyReviewVisibility.ALL) },
            onSuccess = { page ->
                val journal = changes.journal.value
                val isSessionStale = generation != sessionGeneration || currentState.userId != userId
                if (isSessionStale) return@launchResultTask
                val isEpochChanged = baseline.epoch != journal.epoch
                if (isEpochChanged) {
                    refreshReviewCount()
                    return@launchResultTask
                }
                val latest = journal.changesAfter(baseline.revision).mapNotNull { it.change as? ReviewChange.Written }.lastOrNull { it.userId == userId }
                val hasGap = journal.hasGapAfter(baseline.revision)
                if (hasGap) {
                    refreshReviewCount()
                } else {
                    reduce { copy(reviewCount = latest?.counts?.totalCount ?: page.totalCount) }
                }
            },
        )
    }

    private fun refreshBlockedUserCount() {
        val userId = currentState.userId ?: return
        launchResultTask(
            taskKey = BLOCKED_USER_COUNT,
            loadKey = MyTabLoadKey.BlockedUserCount,
            request = userModerationRepository::fetchBlockedUsers,
            onSuccess = { users ->
                val isSameUser = currentState.userId == userId
                if (isSameUser) reduce { copy(blockedUserCount = users.size) }
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
