package com.peto.ramap.ui.main.event

import com.peto.ramap.analytics.AnalyticsEvents
import com.peto.ramap.analytics.AnalyticsParams
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notification.EventNotificationWindow
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.platform.currentEpochMillis
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.event.contract.EventDetailIntent
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnEntered
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnNotificationChanged
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnNotificationPermissionGranted
import com.peto.ramap.ui.main.event.contract.EventDetailLoadKey
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.EventUnavailable
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.RequestNotificationPermission
import com.peto.ramap.ui.main.event.contract.EventDetailUiState
import com.peto.ramap.ui.task.TaskPolicy

class EventDetailViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val loginRepository: LoginRepository,
    private val notificationRepository: NotificationSettingsRepository,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel<EventDetailUiState, EventDetailIntent, EventDetailSideEffect>(EventDetailUiState()) {
    override suspend fun handleIntent(intent: EventDetailIntent) {
        when (intent) {
            is OnEntered -> loadEvent(intent.eventId)
            is OnNotificationChanged -> handleNotificationChanged(intent.enabled)
            OnNotificationPermissionGranted -> updateNotification(enabled = true)
        }
    }

    private fun loadEvent(eventId: String) {
        cancelTask(NOTIFICATION_TASK_KEY)
        launchResultTask(
            taskKey = LOAD_EVENT_TASK_KEY,
            loadKey = EventDetailLoadKey.Fetch,
            policy = TaskPolicy.CancelPrevious,
            request = { ramenShopRepository.fetchActiveEvent(eventId) },
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
        currentState.event?.let { event ->
            analyticsTracker.logEvent(
                AnalyticsEvents.EVENT_UNAVAILABLE,
                mapOf(AnalyticsParams.EVENT_ID to event.id),
            )
        }
        postSideEffect(EventUnavailable)
    }

    private fun updateEventState(event: ShopEvent) {
        analyticsTracker.logEvent(
            AnalyticsEvents.EVENT_DETAIL_VIEW,
            mapOf(
                AnalyticsParams.EVENT_ID to event.id,
                AnalyticsParams.EVENT_TYPE to event.type.name.lowercase(),
                AnalyticsParams.VENUE_SHOP_ID to event.venueShopId,
                AnalyticsParams.IS_TODAY to event.isToday,
            ),
        )
        val notificationWindow = EventNotificationWindow.from(event.startDate, currentEpochMillis())
        val canChangeNotification = loginRepository.hasSession()
        reduce {
            copy(
                event = event,
                isNotificationVisible = notificationWindow != EventNotificationWindow.CLOSED,
                isEventDayOnly = notificationWindow == EventNotificationWindow.EVENT_DAY_ONLY,
                canChangeNotification = canChangeNotification,
            )
        }
    }

    private fun refreshEventNotification(event: ShopEvent) {
        if (!currentState.canChangeNotification || !currentState.isNotificationVisible) return
        launchResultTask(
            taskKey = NOTIFICATION_TASK_KEY,
            loadKey = EventDetailLoadKey.Notification,
            policy = TaskPolicy.CancelPrevious,
            request = { notificationRepository.isEventNotificationEnabled(event.id) },
            onSuccess = ::finishNotificationRefresh,
            onError = { finishNotificationRefresh(isEnabled = false) },
        )
    }

    private fun finishNotificationRefresh(isEnabled: Boolean) {
        reduce {
            copy(
                isNotificationEnabled = isEnabled,
            )
        }
    }

    private suspend fun handleNotificationChanged(enabled: Boolean) {
        currentState.event?.let { event ->
            analyticsTracker.logEvent(
                AnalyticsEvents.EVENT_NOTIFICATION_TOGGLE,
                mapOf(AnalyticsParams.EVENT_ID to event.id),
            )
        }
        if (enabled) {
            postSideEffect(RequestNotificationPermission)
        } else {
            updateNotification(enabled = false)
        }
    }

    private fun updateNotification(enabled: Boolean) {
        val eventId = currentState.event?.id ?: return
        val previousValue = currentState.isNotificationEnabled

        launchResultTask(
            taskKey = NOTIFICATION_TASK_KEY,
            loadKey = EventDetailLoadKey.Notification,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(isNotificationEnabled = enabled) },
            request = { notificationRepository.updateEventNotification(eventId, enabled) },
            onSuccess = { finishNotificationUpdate(isEnabled = enabled) },
            onError = { finishNotificationUpdate(isEnabled = previousValue) },
        )
    }

    private fun finishNotificationUpdate(isEnabled: Boolean) {
        reduce {
            copy(
                isNotificationEnabled = isEnabled,
            )
        }
    }

    companion object {
        private const val LOAD_EVENT_TASK_KEY = "event-detail-load"
        private const val NOTIFICATION_TASK_KEY = "event-detail-notification"
    }
}
