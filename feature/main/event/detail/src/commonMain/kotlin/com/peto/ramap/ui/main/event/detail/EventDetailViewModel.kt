package com.peto.ramap.ui.main.event.detail

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notification.EventNotificationWindow
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.platform.currentEpochMillis
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnCollaboratorInstagramSelected
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnCollaboratorShopSelected
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnEntered
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnNotificationChanged
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnNotificationPermissionGranted
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnRetry
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnSourceLinkSelected
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnVenueShopSelected
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnWaitingLinkSelected
import com.peto.ramap.ui.main.event.detail.contract.EventDetailLoadKey
import com.peto.ramap.ui.main.event.detail.contract.EventDetailSideEffect
import com.peto.ramap.ui.main.event.detail.contract.EventDetailSideEffect.EventUnavailable
import com.peto.ramap.ui.main.event.detail.contract.EventDetailSideEffect.RequestNotificationPermission
import com.peto.ramap.ui.main.event.detail.contract.EventDetailSideEffect.ShowToast
import com.peto.ramap.ui.main.event.detail.contract.EventDetailUiState
import com.peto.ramap.ui.main.event.detail.log.EventDetailAnalytics
import com.peto.ramap.ui.task.TaskPolicy
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_notification_load_failure_message
import ramap.shared.generated.resources.event_notification_update_failure_message

class EventDetailViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val loginRepository: LoginRepository,
    private val notificationRepository: NotificationSettingsRepository,
    private val eventDetailAnalytics: EventDetailAnalytics,
) : BaseViewModel<
        EventDetailUiState,
        EventDetailIntent,
        EventDetailSideEffect,
    >(
        EventDetailUiState(),
    ) {
    override suspend fun handleIntent(intent: EventDetailIntent) {
        when (intent) {
            is OnEntered -> handleEntered(intent)
            OnRetry -> retryEventLoad()
            is OnNotificationChanged -> handleNotificationChanged(intent.enabled)
            OnNotificationPermissionGranted -> handleNotificationPermissionGranted()
            is OnVenueShopSelected -> handleVenueShopSelected(intent.shopId)
            is OnCollaboratorShopSelected -> handleCollaboratorShopSelected(intent.shopId)
            OnCollaboratorInstagramSelected -> handleCollaboratorInstagramSelected()
            OnWaitingLinkSelected -> handleWaitingLinkSelected()
            OnSourceLinkSelected -> handleSourceLinkSelected()
        }
    }

    private fun handleEntered(intent: OnEntered) {
        currentEventId = intent.eventId
        loadEvent(intent.eventId)
    }

    private fun retryEventLoad() {
        val eventId = currentEventId ?: return
        loadEvent(eventId)
    }

    private fun handleNotificationPermissionGranted() {
        updateNotification(enabled = true)
    }

    private fun handleVenueShopSelected(shopId: String) {
        val eventId = currentState.event?.id ?: return

        eventDetailAnalytics.logVenueShopSelected(
            eventId = eventId,
            shopId = shopId,
        )
    }

    private fun handleCollaboratorShopSelected(shopId: String) {
        val eventId = currentState.event?.id ?: return

        eventDetailAnalytics.logCollaboratorShopSelected(
            eventId = eventId,
            shopId = shopId,
        )
    }

    private fun handleCollaboratorInstagramSelected() {
        val eventId = currentState.event?.id ?: return
        eventDetailAnalytics.logCollaboratorInstagramSelected(eventId)
    }

    private fun handleWaitingLinkSelected() {
        val eventId = currentState.event?.id ?: return
        eventDetailAnalytics.logWaitingLinkSelected(eventId)
    }

    private fun handleSourceLinkSelected() {
        val eventId = currentState.event?.id ?: return
        eventDetailAnalytics.logSourceLinkSelected(eventId)
    }

    private fun loadEvent(eventId: String) {
        cancelTask(NOTIFICATION_TASK_KEY)

        launchResultTask(
            taskKey = LOAD_EVENT_TASK_KEY,
            loadKey = EventDetailLoadKey.Fetch,
            policy = TaskPolicy.CancelPrevious,
            onStart = {
                copy(
                    event = null,
                    isNotificationVisible = false,
                    isEventDayOnly = false,
                    canChangeNotification = false,
                    isNotificationEnabled = false,
                    hasEventLoadFailed = false,
                )
            },
            request = {
                ramenShopRepository.fetchEvent(eventId)
            },
            onSuccess = { event ->
                handleLoadedEvent(
                    event = event,
                )
            },
            onError = {
                reduce {
                    copy(
                        event = null,
                        isNotificationVisible = false,
                        isEventDayOnly = false,
                        canChangeNotification = false,
                        isNotificationEnabled = false,
                        hasEventLoadFailed = true,
                    )
                }
            },
        )
    }

    private suspend fun handleLoadedEvent(event: ShopEvent?) {
        if (event == null) {
            showEventUnavailable()
            return
        }

        applyLoadedEvent(event)
        refreshEventNotification(event)
    }

    private suspend fun showEventUnavailable() {
        postSideEffect(EventUnavailable)
    }

    private fun applyLoadedEvent(event: ShopEvent) {
        val notificationWindow =
            EventNotificationWindow.from(
                event.startDate,
                currentEpochMillis(),
            )

        val canChangeNotification =
            loginRepository.hasSession()

        reduce {
            copy(
                event = event,
                isNotificationVisible =
                    notificationWindow !=
                        EventNotificationWindow.CLOSED,
                isEventDayOnly =
                    notificationWindow ==
                        EventNotificationWindow.EVENT_DAY_ONLY,
                canChangeNotification = canChangeNotification,
                hasEventLoadFailed = false,
            )
        }
    }

    private fun refreshEventNotification(event: ShopEvent) {
        if (!currentState.canChangeNotification) {
            return
        }

        if (!currentState.isNotificationVisible) {
            return
        }

        launchResultTask(
            taskKey = NOTIFICATION_TASK_KEY,
            loadKey = EventDetailLoadKey.Notification,
            policy = TaskPolicy.CancelPrevious,
            request = {
                notificationRepository
                    .isEventNotificationEnabled(event.id)
            },
            onSuccess = ::finishNotificationRefresh,
            onError = {
                showNotificationFailure(Res.string.event_notification_load_failure_message)
            },
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
        val eventId =
            currentState.event?.id ?: return

        eventDetailAnalytics.logNotificationToggled(
            eventId = eventId,
            enabled = enabled,
        )

        if (enabled) {
            postSideEffect(RequestNotificationPermission)
            return
        }

        updateNotification(enabled = false)
    }

    private fun updateNotification(enabled: Boolean) {
        val eventId =
            currentState.event?.id ?: return

        val previousValue =
            currentState.isNotificationEnabled

        launchResultTask(
            taskKey = NOTIFICATION_TASK_KEY,
            loadKey = EventDetailLoadKey.Notification,
            policy = TaskPolicy.IgnoreNew,
            onStart = {
                copy(
                    isNotificationEnabled = enabled,
                )
            },
            request = {
                notificationRepository.updateEventNotification(
                    eventId = eventId,
                    enabled = enabled,
                )
            },
            onSuccess = {
                finishNotificationUpdate(
                    isEnabled = enabled,
                )
            },
            onError = {
                finishNotificationUpdate(
                    isEnabled = previousValue,
                )
                showNotificationFailure(Res.string.event_notification_update_failure_message)
            },
        )
    }

    private fun finishNotificationUpdate(isEnabled: Boolean) {
        reduce { copy(isNotificationEnabled = isEnabled) }
    }

    private suspend fun showNotificationFailure(message: org.jetbrains.compose.resources.StringResource) {
        postSideEffect(ShowToast(message))
    }

    companion object {
        private const val LOAD_EVENT_TASK_KEY = "event-detail-load"
        private const val NOTIFICATION_TASK_KEY = "event-detail-notification"
    }

    private var currentEventId: String? = null
}
