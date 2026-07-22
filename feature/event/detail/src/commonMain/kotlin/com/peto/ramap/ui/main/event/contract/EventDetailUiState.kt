package com.peto.ramap.ui.main.event.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class EventDetailUiState(
    val event: ShopEvent? = null,
    val isNotificationVisible: Boolean = false,
    val isEventDayOnly: Boolean = false,
    val canChangeNotification: Boolean = false,
    val isNotificationEnabled: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : State,
    LoadableState<EventDetailUiState> {
    val isEventLoading: Boolean
        get() = loadState.isLoading(EventDetailLoadKey.Fetch)

    val isNotificationLoading: Boolean
        get() = loadState.isLoading(EventDetailLoadKey.Notification)

    override fun withLoadingState(loadState: LoadState): EventDetailUiState = copy(loadState = loadState)
}
