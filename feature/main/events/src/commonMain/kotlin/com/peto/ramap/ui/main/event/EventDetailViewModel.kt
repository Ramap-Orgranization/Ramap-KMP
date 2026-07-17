package com.peto.ramap.ui.main.event

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notification.EventNotificationWindow
import com.peto.ramap.domain.model.notification.eventNotificationWindow
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.platform.currentEpochMillis
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.event.contract.EventDetailIntent
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnEntered
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnNotificationChanged
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnNotificationPermissionGranted
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.EventUnavailable
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.RequestNotificationPermission
import com.peto.ramap.ui.main.event.contract.EventDetailUiState

class EventDetailViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val loginRepository: LoginRepository,
    private val notificationRepository: NotificationSettingsRepository,
) : BaseViewModel<EventDetailUiState, EventDetailIntent, EventDetailSideEffect>(EventDetailUiState()) {
    override suspend fun handleIntent(intent: EventDetailIntent) {
        when (intent) {
            is OnEntered -> loadEvent(intent.eventId)
            is OnNotificationChanged -> handleNotificationChanged(intent.enabled)
            OnNotificationPermissionGranted -> updateNotification(enabled = true)
        }
    }

    private suspend fun loadEvent(eventId: String) {
        handleResult(
            result = ramenShopRepository.fetchActiveEvent(eventId),
            onSuccess = ::handleLoadedEvent,
            onError = { showEventUnavailable() },
        )
    }

    private suspend fun handleLoadedEvent(event: ShopEvent?) {
        if (event == null) return showEventUnavailable()
        updateEventState(event)
        refreshEventNotification(event)
    }

    private suspend fun showEventUnavailable() {
        postSideEffect(EventUnavailable)
    }

    private fun updateEventState(event: ShopEvent) {
        val notificationWindow = eventNotificationWindow(event.startDate, currentEpochMillis())
        val canChangeNotification = loginRepository.hasSession()
        reduce {
            copy(
                event = event,
                isNotificationVisible = notificationWindow != EventNotificationWindow.CLOSED,
                isEventDayOnly = notificationWindow == EventNotificationWindow.EVENT_DAY_ONLY,
                canChangeNotification = canChangeNotification,
                isNotificationLoading = canChangeNotification,
            )
        }
    }

    private suspend fun refreshEventNotification(event: ShopEvent) {
        if (!currentState.canChangeNotification || !currentState.isNotificationVisible) return
        handleResult(
            result = notificationRepository.isEventNotificationEnabled(event.id),
            onSuccess = ::finishNotificationRefresh,
            onError = { finishNotificationRefresh(isEnabled = false) },
        )
    }

    private fun finishNotificationRefresh(isEnabled: Boolean) {
        reduce {
            copy(
                isNotificationEnabled = isEnabled,
                isNotificationLoading = false,
            )
        }
    }

    private suspend fun handleNotificationChanged(enabled: Boolean) {
        if (enabled) {
            postSideEffect(RequestNotificationPermission)
        } else {
            updateNotification(enabled = false)
        }
    }

    private suspend fun updateNotification(enabled: Boolean) {
        val eventId = currentState.event?.id ?: return
        val previousValue = currentState.isNotificationEnabled
        reduce { copy(isNotificationEnabled = enabled, isNotificationLoading = true) }

        handleResult(
            result = notificationRepository.updateEventNotification(eventId, enabled),
            onSuccess = { finishNotificationUpdate(isEnabled = enabled) },
            onError = { finishNotificationUpdate(isEnabled = previousValue) },
        )
    }

    private fun finishNotificationUpdate(isEnabled: Boolean) {
        reduce {
            copy(
                isNotificationEnabled = isEnabled,
                isNotificationLoading = false,
            )
        }
    }
}
