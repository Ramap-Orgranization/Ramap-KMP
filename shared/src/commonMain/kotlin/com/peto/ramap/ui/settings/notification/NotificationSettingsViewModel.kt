package com.peto.ramap.ui.settings.notification

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.base.BaseViewModel
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.ui.settings.notification.contract.NotificationRemovalTarget
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnEnabledChanged
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnEventOverrideRemoved
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalConfirmed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalDismissed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalRequested
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnResumed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnShopRemoved
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsSideEffect
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsSideEffect.ShowToast
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsUiState
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.notification_permission_enable_message

class NotificationSettingsViewModel(
    private val notificationRepository: NotificationSettingsRepository,
    private val ramenShopRepository: RamenShopRepository,
    private val requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
    private val isNotificationPermissionGranted: suspend () -> Boolean = NotificationPermissionRequester::isGranted,
) : BaseViewModel<NotificationSettingsUiState, NotificationSettingsIntent, NotificationSettingsSideEffect>(
        initialState = NotificationSettingsUiState(),
    ) {
    init {
        viewModelScope.launch { loadSettings() }
    }

    override suspend fun handleIntent(intent: NotificationSettingsIntent) {
        when (intent) {
            OnResumed -> refreshEnabled()
            is OnEnabledChanged -> updateEnabled(intent.enabled)
            is OnShopRemoved -> removeShop(intent.shopId)
            is OnEventOverrideRemoved -> removeEventOverride(intent.eventId)
            is OnRemovalRequested -> reduce { copy(pendingRemoval = intent.target) }
            OnRemovalDismissed -> reduce { copy(pendingRemoval = null) }
            OnRemovalConfirmed -> confirmRemoval()
        }
    }

    private suspend fun confirmRemoval() {
        when (val target = currentState.pendingRemoval) {
            is NotificationRemovalTarget.Shop -> removeShop(target.shopId)
            is NotificationRemovalTarget.EventOverride -> removeEventOverride(target.eventId)
            null -> return
        }
        reduce { copy(pendingRemoval = null) }
    }

    private suspend fun loadSettings() {
        val enabled = notificationRepository.isEnabled()
        val shopIds = notificationRepository.fetchSubscribedShopIds()
        val overrides = notificationRepository.fetchEventOverrides()
        val subscribedShopIds = (shopIds as? RamapResult.Success)?.data.orEmpty()
        val eventOverrides = (overrides as? RamapResult.Success)?.data.orEmpty()
        val shops = loadShops(subscribedShopIds)
        val events =
            eventOverrides.mapNotNull { override ->
                (ramenShopRepository.fetchActiveEvent(override.eventId) as? RamapResult.Success)
                    ?.data
                    ?.let { override to it }
            }
        val areEnabled = enabled.isEnabledWithPermission()
        reduce {
            copy(
                areEnabled = areEnabled,
                subscribedShopCount = subscribedShopIds.size,
                shops = shops.sortedBy { it.name },
                subscribedEvents = events.filter { it.first.enabled }.map { it.second },
            )
        }
    }

    private suspend fun refreshEnabled() {
        val enabled = notificationRepository.isEnabled()
        val areEnabled = enabled.isEnabledWithPermission()
        reduce { copy(areEnabled = areEnabled) }
    }

    private suspend fun RamapResult<Boolean>.isEnabledWithPermission(): Boolean =
        (this as? RamapResult.Success)?.data == true && isNotificationPermissionGranted()

    private suspend fun loadShops(shopIds: Set<String>) =
        if (shopIds.isEmpty()) {
            emptyList()
        } else {
            (ramenShopRepository.fetchRamenShopsByIds(shopIds) as? RamapResult.Success)
                ?.data
                ?.values
                .orEmpty()
        }

    private suspend fun removeShop(shopId: String) {
        if (notificationRepository.updateShopNotification(shopId, false) !is RamapResult.Success) return
        reduce {
            copy(
                shops = shops.filterNot { it.id == shopId },
                subscribedShopCount = (subscribedShopCount - 1).coerceAtLeast(0),
            )
        }
    }

    private suspend fun removeEventOverride(eventId: String) {
        if (notificationRepository.clearEventNotificationOverride(eventId) !is RamapResult.Success) return
        reduce {
            copy(
                subscribedEvents = subscribedEvents.filterNot { it.id == eventId },
            )
        }
    }

    private suspend fun updateEnabled(enabled: Boolean) {
        if (enabled && !requestNotificationPermission()) {
            trySideEffect(
                ShowToast(
                    ToastData(
                        message = Res.string.notification_permission_enable_message,
                        type = ToastType.DEFAULT,
                        action = ToastAction(label = Res.string.location_permission_settings_action),
                    ),
                ),
            )
            return
        }
        val previous = currentState.areEnabled
        reduce { copy(areEnabled = enabled) }
        if (notificationRepository.updateEnabled(enabled) is RamapResult.Error) {
            reduce { copy(areEnabled = previous) }
        }
    }
}
