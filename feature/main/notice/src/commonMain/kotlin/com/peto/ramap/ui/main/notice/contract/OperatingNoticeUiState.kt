package com.peto.ramap.ui.main.notice.contract

import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class OperatingNoticeUiState(
    val todayOperatingNotices: List<OperatingNotice> = emptyList(),
    val scheduledOperatingNotices: List<OperatingNotice> = emptyList(),
    val showOperatingNoticeError: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : State,
    LoadableState<OperatingNoticeUiState> {
    val isLoading: Boolean
        get() = loadState.isLoading(OperatingNoticeLoadKey.Fetch)

    val isRefreshing: Boolean
        get() = loadState.isLoading(OperatingNoticeLoadKey.Refresh)

    val hasOperatingNotices: Boolean
        get() = todayOperatingNotices.isNotEmpty() || scheduledOperatingNotices.isNotEmpty()

    override fun withLoadingState(loadState: LoadState): OperatingNoticeUiState = copy(loadState = loadState)
}
