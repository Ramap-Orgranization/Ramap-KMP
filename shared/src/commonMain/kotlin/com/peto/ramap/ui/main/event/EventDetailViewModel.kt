package com.peto.ramap.ui.main.event

import com.peto.ramap.core.base.BaseViewModel
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.EventNotificationWindow
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.domain.model.eventNotificationWindow
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.platform.currentEpochMillis
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
            is OnEntered -> loadEvent(intent.eventId, intent.initialEvent)
            is OnNotificationChanged -> updateNotification(intent.enabled)
        }
    }

    private suspend fun loadEvent(
        eventId: String,
        initialEvent: ShopEvent?,
    ) {
        if (currentState.event?.id == eventId) return
        reduce { EventDetailUiState() }
        val event = initialEvent ?: (ramenShopRepository.fetchActiveEvent(eventId) as? RamapResult.Success)?.data
        if (event == null) return postSideEffect(EventUnavailable)

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
        if (!canChangeNotification || notificationWindow == EventNotificationWindow.CLOSED) return
        val result = notificationRepository.isEventNotificationEnabled(event.id)
        reduce {
            copy(
                isNotificationEnabled = (result as? RamapResult.Success)?.data ?: false,
                isNotificationLoading = false,
            )
        }
    }

    private suspend fun updateNotification(enabled: Boolean) {
        val event = currentState.event ?: return
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
            return
        }
        val previousValue = currentState.isNotificationEnabled
        reduce { copy(isNotificationEnabled = enabled, isNotificationLoading = true) }
        val result = notificationRepository.updateEventNotification(event.id, enabled)
        reduce {
            copy(
                isNotificationEnabled = if (result is RamapResult.Error) previousValue else enabled,
                isNotificationLoading = false,
            )
        }
    }
}
