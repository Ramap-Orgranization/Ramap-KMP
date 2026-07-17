package com.peto.ramap.ui.main.event

import com.peto.ramap.core.result.RamapResult
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
        val event = (ramenShopRepository.fetchActiveEvent(eventId) as? RamapResult.Success)?.data
        if (event == null) return postSideEffect(EventUnavailable)

        updateEventState(event)
        refreshEventNotification(event)
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
        val result = notificationRepository.isEventNotificationEnabled(event.id)
        reduce {
            copy(
                isNotificationEnabled = (result as? RamapResult.Success)?.data ?: false,
                isNotificationLoading = false,
            )
        }
    }

    private suspend fun updateNotification(enabled: Boolean) {
        val eventId = currentState.event?.id ?: return
        val previousValue = currentState.isNotificationEnabled
        if (!prepareNotificationUpdate(enabled)) return

        val result = saveEventNotification(eventId, enabled)
        handleNotificationUpdateResult(result, enabled, previousValue)
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

    private suspend fun saveEventNotification(
        eventId: String,
        enabled: Boolean,
    ): RamapResult<Unit> = notificationRepository.updateEventNotification(eventId, enabled)

    private fun handleNotificationUpdateResult(
        result: RamapResult<Unit>,
        enabled: Boolean,
        previousValue: Boolean,
    ) {
        reduce {
            copy(
                isNotificationEnabled = if (result is RamapResult.Error) previousValue else enabled,
                isNotificationLoading = false,
            )
        }
    }
}
