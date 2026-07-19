package com.peto.ramap.ui.main.event.list

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.event.list.contract.EventsUiState
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_refresh_failure_message

class EventsViewModel(
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<EventsUiState, EventsIntent, EventsSideEffect>(EventsUiState()) {
    init {
        viewModelScope.launch { loadEvents() }
    }

    override suspend fun handleIntent(intent: EventsIntent) {
        when (intent) {
            EventsIntent.OnEventsRefreshed -> refreshEvents()
            EventsIntent.OnEventsRetried -> loadEvents()
        }
    }

    private suspend fun loadEvents() {
        reduce { copy(eventsState = LoadState.Loading) }
        handleResult(
            result = ramenShopRepository.fetchActiveEvents(),
            onSuccess = { events -> reduce { copy(eventsState = LoadState.Content(events), isRefreshing = false) } },
            onError = { reduce { copy(eventsState = LoadState.Error) } },
        )
    }

    private suspend fun refreshEvents() {
        reduce { copy(isRefreshing = true) }
        handleResult(
            result = ramenShopRepository.fetchActiveEvents(),
            onSuccess = { events -> reduce { copy(eventsState = LoadState.Content(events), isRefreshing = false) } },
            onError = {
                reduce { copy(isRefreshing = false) }
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
}
