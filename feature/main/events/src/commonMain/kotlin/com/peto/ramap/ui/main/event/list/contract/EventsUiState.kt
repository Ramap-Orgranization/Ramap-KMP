package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.common.RamapUiState
import com.peto.ramap.ui.loading.LoadableState
import com.peto.ramap.ui.loading.LoadState as TaskLoadState

data class EventsUiState(
    val eventsState: RamapUiState<List<ShopEvent>> = RamapUiState.Idle,
    /** 이벤트 화면의 작업별 로딩 카운트. */
    override val loadState: TaskLoadState = TaskLoadState(),
) : State,
    LoadableState<EventsUiState> {
    /** 기존 목록을 유지한 채 새로고침 요청이 진행 중인지 여부. */
    val isRefreshing: Boolean
        get() = loadState.isLoading(EventsLoadKey.Refresh)

    /** 로딩 카운트만 교체한 새 이벤트 UI 상태를 반환한다. */
    override fun withLoadingState(loadState: TaskLoadState): EventsUiState = copy(loadState = loadState)
}
