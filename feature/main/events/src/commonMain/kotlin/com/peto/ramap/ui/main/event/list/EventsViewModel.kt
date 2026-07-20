package com.peto.ramap.ui.main.event.list

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.RamapUiState
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsLoadKey
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.event.list.contract.EventsUiState
import com.peto.ramap.ui.task.TaskPolicy
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_refresh_failure_message

class EventsViewModel(
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<EventsUiState, EventsIntent, EventsSideEffect>(EventsUiState()) {
    init {
        loadEvents()
    }

    override suspend fun handleIntent(intent: EventsIntent) {
        when (intent) {
            EventsIntent.OnEventsRefreshed -> refreshEvents()
            EventsIntent.OnEventsRetried -> loadEvents()
        }
    }

    /** 최초 조회와 재시도를 동일 작업으로 실행해 진행 중 요청을 새 요청으로 교체한다. */
    private fun loadEvents() {
        launchResultTask(
            taskKey = EVENTS_TASK_KEY,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(eventsState = RamapUiState.Loading) },
            request = ramenShopRepository::fetchActiveEvents,
            onSuccess = { events -> reduce { copy(eventsState = RamapUiState.Success(events)) } },
            onError = { reduce { copy(eventsState = RamapUiState.Error) } },
        )
    }

    /** 진행 중인 이벤트 조회를 교체하되 기존 콘텐츠는 유지하고 새로고침 로딩만 노출한다. */
    private fun refreshEvents() {
        launchResultTask(
            taskKey = EVENTS_TASK_KEY,
            loadKey = EventsLoadKey.Refresh,
            policy = TaskPolicy.CancelPrevious,
            request = ramenShopRepository::fetchActiveEvents,
            onSuccess = { events -> reduce { copy(eventsState = RamapUiState.Success(events)) } },
            onError = {
                trySideEffect(
                    EventsSideEffect.ShowEventsToast(
                        ToastData(
                            message = Res.string.event_list_refresh_failure_message,
                            type = ToastType.ERROR,
                        ),
                    ),
                )
            },
        )
    }

    companion object {
        /** 최초 조회·재시도·새로고침이 서로 교체되도록 공유하는 작업 키. */
        private const val EVENTS_TASK_KEY = "events"
    }
}
