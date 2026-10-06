package com.peto.ramap.ui.profile.follow

import androidx.lifecycle.viewModelScope
import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowList
import com.peto.ramap.domain.repository.FollowRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class FollowViewModel(
    private val follows: FollowRepository,
    private val profiles: ProfileRepository,
) : BaseViewModel<FollowState, FollowIntent, FollowEffect>(FollowState()) {
    init {
        viewModelScope.launch {
            profiles.sessionUserIds.distinctUntilChanged().collect { userId ->
                cancelTask(PAGE_TASK)
                cancelTask(MUTATION_TASK)
                cancelTask(COUNTS_TASK)
                reduce { FollowState(currentUserId = userId) }
                if (userId != null) refresh()
            }
        }
    }

    override suspend fun handleIntent(intent: FollowIntent) {
        when (intent) {
            is FollowIntent.Select -> if (!currentState.mutating) selectList(intent.list)
            is FollowIntent.Change -> changeFollow(intent)
            FollowIntent.Retry -> refresh()
            FollowIntent.ReturnedFromProfile -> refresh()
            FollowIntent.LoadMore -> if (currentState.hasMore && !currentState.failed && !currentState.mutating) loadPage(reset = false)
        }
    }

    private fun refresh() {
        if (currentState.mutating) return
        loadCounts()
        loadPage(reset = true)
    }

    private fun selectList(list: FollowList) {
        if (currentState.selectedList == list || list !in currentState.availableLists) return
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
                if (currentState.currentUserId != userId) return@launchResultTask
                reduce { copy(counts = counts, countsFailed = false) }
                if (counts.requests == 0L && currentState.selectedList == FollowList.REQUESTS) selectList(FollowList.FOLLOWING)
            },
            onError = { if (currentState.currentUserId == userId) reduce { copy(countsFailed = true) } },
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
                if (currentState.currentUserId == userId && currentState.selectedList == list) {
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
                if (currentState.currentUserId == userId && currentState.selectedList == list) {
                    reduce { copy(failed = true) }
                }
            },
        )
    }

    private fun changeFollow(intent: FollowIntent.Change) {
        val userId = currentState.currentUserId ?: return
        if (currentState.mutating) return
        val list = currentState.selectedList
        if (currentState.profiles.none { it.userId == intent.userId } || !isListAction(list, intent.action)) return
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
                if (currentState.currentUserId != userId) return@launchResultTask
                cancelTask(PAGE_TASK)
                if (currentState.selectedList != list) {
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
                if (currentState.selectedList == FollowList.REQUESTS && currentState.counts?.requests == 0L) {
                    selectList(FollowList.FOLLOWING)
                } else if (currentState.profiles.isEmpty() && currentState.hasMore) {
                    loadPage(reset = false)
                }
                loadCounts()
            },
            onError = {
                if (currentState.currentUserId == userId) {
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
