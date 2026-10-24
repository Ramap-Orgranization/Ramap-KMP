package com.peto.ramap.ui.profile.follow

import androidx.lifecycle.viewModelScope
import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowList
import com.peto.ramap.domain.repository.FollowRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.profile.follow.contract.FollowEffect
import com.peto.ramap.ui.profile.follow.contract.FollowIntent
import com.peto.ramap.ui.profile.follow.contract.FollowLoadKey
import com.peto.ramap.ui.profile.follow.contract.FollowState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class FollowViewModel(
    private val follows: FollowRepository,
    private val profiles: ProfileRepository,
) : BaseViewModel<FollowState, FollowIntent, FollowEffect>(FollowState()) {
    init {
        viewModelScope.launch {
            profiles.sessionUserId.distinctUntilChanged().collect { userId ->
                cancelTask(PAGE_TASK)
                cancelTask(MUTATION_TASK)
                cancelTask(COUNTS_TASK)
                reduce { FollowState(currentUserId = userId) }
                val hasUserId = userId != null
                if (hasUserId) refresh()
            }
        }
    }

    override suspend fun handleIntent(intent: FollowIntent) {
        when (intent) {
            is FollowIntent.Select -> {
                val isNotMutating = !currentState.mutating
                if (isNotMutating) selectList(intent.list)
            }
            is FollowIntent.Change -> changeFollow(intent)
            FollowIntent.Retry -> refresh()
            FollowIntent.ReturnedFromProfile -> refresh()
            FollowIntent.LoadMore -> {
                val canLoadMore = currentState.hasMore && !currentState.failed && !currentState.mutating
                if (canLoadMore) loadPage(reset = false)
            }
        }
    }

    private fun refresh() {
        val isMutating = currentState.mutating
        if (isMutating) return
        loadCounts()
        loadPage(reset = true)
    }

    private fun selectList(list: FollowList) {
        val isSameList = currentState.selectedList == list
        val isListUnavailable = list !in currentState.availableLists
        if (isSameList || isListUnavailable) return
        cancelTask(PAGE_TASK)
        reduce { copy(selectedList = list, profiles = emptyList(), offset = 0L, hasMore = false, failed = false) }
        loadPage(reset = true)
    }

    private fun loadCounts() {
        val userId = currentState.currentUserId ?: return
        launchResultTask(
            taskKey = COUNTS_TASK,
            loadKey = FollowLoadKey.Counts,
            onStart = { copy(countsFailed = false) },
            request = follows::fetchCounts,
            onSuccess = { counts ->
                val isUserChanged = currentState.currentUserId != userId
                if (isUserChanged) return@launchResultTask
                reduce { copy(counts = counts, countsFailed = false) }
                val isRequestsEmpty = counts.requests == 0L && currentState.selectedList == FollowList.REQUESTS
                if (isRequestsEmpty) selectList(FollowList.FOLLOWING)
            },
            onError = {
                val isSameUser = currentState.currentUserId == userId
                if (isSameUser) reduce { copy(countsFailed = true) }
            },
        )
    }

    private fun loadPage(reset: Boolean) {
        val userId = currentState.currentUserId ?: return
        val list = currentState.selectedList
        val offset = if (reset) 0L else currentState.offset
        launchResultTask(
            taskKey = PAGE_TASK,
            loadKey = FollowLoadKey.Page,
            policy = if (reset) TaskPolicy.CancelPrevious else TaskPolicy.IgnoreNew,
            onStart = { copy(failed = false) },
            request = { follows.fetchConnections(list, offset) },
            onSuccess = { page ->
                val isCurrentContext = currentState.currentUserId == userId && currentState.selectedList == list
                if (isCurrentContext) {
                    reduce {
                        copy(
                            profiles = (if (reset) page else profiles + page).distinctBy { it.userId },
                            offset = offset + page.size,
                            hasMore = page.size == PAGE_SIZE,
                        )
                    }
                }
            },
            onError = {
                val isCurrentContext = currentState.currentUserId == userId && currentState.selectedList == list
                if (isCurrentContext) {
                    reduce { copy(failed = true) }
                }
            },
        )
    }

    private fun changeFollow(intent: FollowIntent.Change) {
        val userId = currentState.currentUserId ?: return
        val isMutating = currentState.mutating
        if (isMutating) return
        val list = currentState.selectedList
        val isUserNotInProfiles = currentState.profiles.none { it.userId == intent.userId }
        val isInvalidAction = !isListAction(list, intent.action)
        if (isUserNotInProfiles || isInvalidAction) return
        cancelTask(PAGE_TASK)
        cancelTask(COUNTS_TASK)
        launchResultTask(
            taskKey = MUTATION_TASK,
            loadKey = FollowLoadKey.Mutation,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(actingUserId = intent.userId, actingAction = intent.action) },
            onFinish = { copy(actingUserId = null, actingAction = null) },
            request = { follows.changeFollow(intent.userId, intent.action) },
            onSuccess = {
                val isUserChanged = currentState.currentUserId != userId
                if (isUserChanged) return@launchResultTask
                cancelTask(PAGE_TASK)
                val isListChanged = currentState.selectedList != list
                if (isListChanged) {
                    loadPage(reset = true)
                    return@launchResultTask
                }
                reduce {
                    val remaining = profiles.filterNot { it.userId == intent.userId }
                    copy(
                        profiles = remaining,
                        offset = (offset - (profiles.size - remaining.size)).coerceAtLeast(0L),
                        counts = counts?.after(intent.action),
                    )
                }
                val isRequestsListEmpty = currentState.selectedList == FollowList.REQUESTS && currentState.counts?.requests == 0L
                val shouldLoadMore = currentState.profiles.isEmpty() && currentState.hasMore
                if (isRequestsListEmpty) {
                    selectList(FollowList.FOLLOWING)
                } else if (shouldLoadMore) {
                    loadPage(reset = false)
                }
                loadCounts()
            },
            onError = {
                val isSameUser = currentState.currentUserId == userId
                if (isSameUser) {
                    loadCounts()
                    postSideEffect(FollowEffect.ActionFailed)
                }
            },
        )
    }

    private fun isListAction(
        list: FollowList,
        action: FollowAction,
    ): Boolean =
        when (list) {
            FollowList.FOLLOWERS -> action == FollowAction.REMOVE
            FollowList.FOLLOWING -> action == FollowAction.UNFOLLOW
            FollowList.REQUESTS -> action == FollowAction.APPROVE || action == FollowAction.REJECT
        }

    private companion object {
        const val PAGE_TASK = "follow-connections"
        const val MUTATION_TASK = "follow-mutation"
        const val COUNTS_TASK = "follow-counts"
        const val PAGE_SIZE = 20
    }
}
