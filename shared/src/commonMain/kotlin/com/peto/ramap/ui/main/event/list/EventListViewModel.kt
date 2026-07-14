package com.peto.ramap.ui.main.event.list

import com.peto.ramap.core.base.BaseViewModel
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.main.event.list.contract.EventListIntent
import com.peto.ramap.ui.main.event.list.contract.EventListSideEffect
import com.peto.ramap.ui.main.event.list.contract.EventListUiState
import com.peto.ramap.ui.main.event.list.contract.OnEventListEntered
import com.peto.ramap.ui.main.event.list.contract.OnEventListRefreshed
import com.peto.ramap.ui.main.event.list.contract.OnEventListRetried

class EventListViewModel(
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<EventListUiState, EventListIntent, EventListSideEffect>(EventListUiState()) {
    override suspend fun handleIntent(intent: EventListIntent) {
        when (intent) {
            OnEventListEntered -> if (currentState.eventsState == LoadState.Idle) loadEvents()
            OnEventListRefreshed -> refreshEvents()
            OnEventListRetried -> loadEvents()
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
            onError = { reduce { copy(isRefreshing = false) } },
        )
    }
}
