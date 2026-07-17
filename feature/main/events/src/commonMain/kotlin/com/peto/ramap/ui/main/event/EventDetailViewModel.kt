package com.peto.ramap.ui.main.event

import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notification.EventNotificationWindow
import com.peto.ramap.domain.model.notification.eventNotificationWindow
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.platform.currentEpochMillis
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.event.contract.EventDetailIntent
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnEntered
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnNotificationChanged
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.EventUnavailable
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.ShowEventToast
import com.peto.ramap.ui.main.event.contract.EventDetailUiState
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.notification_permission_enable_message

class EventDetailViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val loginRepository: LoginRepository,
    private val notificationRepository: NotificationSettingsRepository,
    private val requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
) : BaseViewModel<EventDetailUiState, EventDetailIntent, EventDetailSideEffect>(EventDetailUiState()) {
    override suspend fun handleIntent(intent: EventDetailIntent) {
        when (intent) {
            is OnEntered -> loadEvent(intent.eventId)
            is OnNotificationChanged -> updateNotification(intent.enabled)
        }
    }

    private suspend fun loadEvent(eventId: String) {
        reduce { EventDetailUiState() }
        handleResult(
            result = ramenShopRepository.fetchActiveEvent(eventId),
            onSuccess = { event ->
                if (event == null) {
                    postSideEffect(EventUnavailable)
                    return@handleResult
                }
                updateEventState(event)
                refreshEventNotification(event)
            },
            onError = {
                postSideEffect(EventUnavailable)
            },
        )
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
            onSuccess = { isEnabled ->
                reduce {
                    copy(
                        isNotificationEnabled = isEnabled,
                        isNotificationLoading = false,
                    )
                }
            },
            onError = {
                reduce {
                    copy(
                        isNotificationEnabled = false,
                        isNotificationLoading = false,
                    )
                }
            },
        )
    }

    private suspend fun updateNotification(enabled: Boolean) {
        val eventId = currentState.event?.id ?: return
        val previousValue = currentState.isNotificationEnabled
        if (!prepareNotificationUpdate(enabled)) return

        handleResult(
            result = notificationRepository.updateEventNotification(eventId, enabled),
            onSuccess = {
                finishNotificationUpdate(enabled)
            },
            onError = {
                finishNotificationUpdate(previousValue)
            },
        )
    }

    private suspend fun prepareNotificationUpdate(enabled: Boolean): Boolean {
        if (enabled && !requestNotificationPermission()) {
            postSideEffect(
                ShowEventToast(
                    ToastData(
                        message = Res.string.notification_permission_enable_message,
                        type = ToastType.DEFAULT,
                        action = ToastAction(label = Res.string.location_permission_settings_action),
                    ),
                ),
            )
            return false
        }
        reduce { copy(isNotificationEnabled = enabled, isNotificationLoading = true) }
        return true
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
