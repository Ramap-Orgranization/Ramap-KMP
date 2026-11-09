package com.peto.ramap.ui.announcement.contract

import com.peto.ramap.domain.model.announcement.Announcement
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class AnnouncementUiState(
    val announcements: List<Announcement> = emptyList(),
    val readIds: Set<String> = emptySet(),
    val readAccountId: String? = null,
    val selected: Announcement? = null,
    val failed: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<AnnouncementUiState> {
    val loading: Boolean get() = loadState.isLoading(AnnouncementLoadKey.Fetch)

    val isRefreshing: Boolean get() = loadState.isLoading(AnnouncementLoadKey.Refresh)

    override fun withLoadingState(loadState: LoadState): AnnouncementUiState = copy(loadState = loadState)
}
