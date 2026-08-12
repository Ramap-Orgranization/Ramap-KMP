package com.peto.ramap.ui.main.event.list

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsLoadKey
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.event.list.contract.EventsUiState
import com.peto.ramap.ui.main.event.list.contract.mapEventsToUiState
import com.peto.ramap.ui.main.event.list.log.EventsAnalytics
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
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
            onSuccess = { events -> reduce { mapEventsToUiState(this, events) } },
            onError = { reduce { copy(showError = true) } },
        )
    }

    private fun refreshEvents() {
        launchResultTask(
            taskKey = EVENTS_TASK_KEY,
            loadKey = EventsLoadKey.Refresh,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(showError = false) },
            retryOnNetworkError = true,
            request = ramenShopRepository::fetchActiveEvents,
            onSuccess = { events -> reduce { mapEventsToUiState(this, events) } },
            onError = {
                showToast(
                    message = Res.string.event_list_refresh_failure_message,
                    type = ToastType.ERROR,
                )
            },
        )
    }

    private fun showToast(
        message: StringResource,
        type: ToastType = ToastType.ERROR,
    ) {
        viewModelScope.launch {
            trySideEffect(EventsSideEffect.ShowEventsToast(ToastData(message, type)))
        }
    }

    companion object {
        private const val EVENTS_TASK_KEY = "events"
    }
}
