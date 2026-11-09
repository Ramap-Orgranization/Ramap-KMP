package com.peto.ramap.ui.announcement

import com.peto.ramap.domain.repository.AnnouncementRepository
import com.peto.ramap.domain.store.AnnouncementReadStore
import com.peto.ramap.ui.announcement.contract.AnnouncementIntent
import com.peto.ramap.ui.announcement.contract.AnnouncementLoadKey
import com.peto.ramap.ui.announcement.contract.AnnouncementUiState
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.base.SideEffect
import com.peto.ramap.ui.task.TaskPolicy

class AnnouncementViewModel(
    private val repository: AnnouncementRepository,
    private val readStore: AnnouncementReadStore,
) : BaseViewModel<AnnouncementUiState, AnnouncementIntent, SideEffect>(AnnouncementUiState()) {
    init {
        launchTask(taskKey = OBSERVE_READ) {
            readStore.state.collect { reads ->
                reduce {
                    copy(
                        readIds = reads.readIds,
                        readAccountId = reads.accountId,
                        selected = selected.takeIf { readAccountId == reads.accountId },
                    )
                }
            }
        }
        refresh()
    }

    override suspend fun handleIntent(intent: AnnouncementIntent) {
        when (intent) {
            AnnouncementIntent.Refresh -> refresh(AnnouncementLoadKey.Refresh)
            AnnouncementIntent.ReturnToScreen -> refresh(loadKey = null)
            AnnouncementIntent.Retry -> refresh()
            is AnnouncementIntent.Open -> open(intent.id)
            AnnouncementIntent.Dismiss -> reduce { copy(selected = null) }
        }
    }

    private fun refresh(loadKey: AnnouncementLoadKey? = AnnouncementLoadKey.Fetch) {
        launchResultTask(
            taskKey = REFRESH_READS,
            request = readStore::refresh,
        )
        launchResultTask(
            taskKey = FETCH,
            loadKey = loadKey,
            policy = if (loadKey == null) TaskPolicy.IgnoreNew else TaskPolicy.CancelPrevious,
            onStart = { if (loadKey == null) this else copy(failed = false) },
            request = repository::fetchAnnouncements,
            onSuccess = { announcements ->
                reduce {
                    copy(
                        announcements = announcements,
                        selected = announcements.find { it.id == selected?.id },
                        failed = false,
                    )
                }
            },
            onError = { reduce { copy(failed = true) } },
        )
    }

    private fun open(id: String) {
        val announcement = currentState.announcements.find { it.id == id } ?: return
        reduce { copy(selected = announcement) }
        val accountId = currentState.readAccountId
        launchResultTask(
            taskKey = "$MARK_READ$accountId:$id",
            policy = TaskPolicy.IgnoreNew,
            request = { readStore.markRead(id, accountId) },
            retryOnNetworkError = true,
        )
    }

    private companion object {
        const val FETCH = "announcements-fetch"
        const val OBSERVE_READ = "announcements-observe-read"
        const val REFRESH_READS = "announcements-refresh-reads"
        const val MARK_READ = "announcements-mark-read-"
    }
}
