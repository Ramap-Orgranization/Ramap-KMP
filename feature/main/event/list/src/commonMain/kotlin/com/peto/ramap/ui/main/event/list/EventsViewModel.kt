package com.peto.ramap.ui.main.event.list

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsLoadKey
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.event.list.contract.EventsUiState
import com.peto.ramap.ui.main.event.list.log.EventsAnalytics
import com.peto.ramap.ui.task.TaskPolicy
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_refresh_failure_message

class EventsViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val eventsAnalytics: EventsAnalytics,
) : BaseViewModel<EventsUiState, EventsIntent, EventsSideEffect>(EventsUiState()) {
    init {
        loadEvents()
    }

    override suspend fun handleIntent(intent: EventsIntent) {
        when (intent) {
            EventsIntent.OnEventsRefreshed -> refreshEvents()
            EventsIntent.OnEventsRetried -> loadEvents()
            is EventsIntent.OnEventClicked -> eventsAnalytics.logEventSelected(intent.event)
        }
    }

    private fun loadEvents() {
        launchResultTask(
            taskKey = EVENTS_TASK_KEY,
            loadKey = EventsLoadKey.Fetch,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(showError = false) },
            retryOnNetworkError = true,
            request = ramenShopRepository::fetchActiveEvents,
            onSuccess = { events -> reduce { copy(events = events, showError = false) } },
            onError = { reduce { copy(showError = true) } },
        )
    }

    private fun refreshEvents() {
        launchResultTask(
            taskKey = EVENTS_TASK_KEY,
            loadKey = EventsLoadKey.Refresh,
            policy = TaskPolicy.CancelPrevious,
            retryOnNetworkError = true,
            request = ramenShopRepository::fetchActiveEvents,
            onSuccess = { events -> reduce { copy(events = events, showError = false) } },
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
        private const val EVENTS_TASK_KEY = "events"
    }
}
