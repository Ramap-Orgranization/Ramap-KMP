package com.peto.ramap.ui.settings.notification

import androidx.lifecycle.viewModelScope
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnEventNotificationsEnabledChanged
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnEventOverrideRemoved
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalConfirmed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalDismissed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalRequested
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnShopRemoved
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsSideEffect
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsUiState
import com.peto.ramap.ui.settings.notification.model.NotificationRemovalTarget
import kotlinx.coroutines.launch

class NotificationSettingsViewModel(
    private val notificationRepository: NotificationSettingsRepository,
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<NotificationSettingsUiState, NotificationSettingsIntent, NotificationSettingsSideEffect>(
        initialState = NotificationSettingsUiState(),
    ) {
    init {
        viewModelScope.launch { loadSettings() }
    }

    override suspend fun handleIntent(intent: NotificationSettingsIntent) {
        when (intent) {
            is OnEventNotificationsEnabledChanged -> updateEnabled(intent.enabled)
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
        reduce {
            copy(
                areEnabled = (enabled as? RamapResult.Success)?.data == true,
                subscribedShopCount = subscribedShopIds.size,
                shops = shops.sortedByName(),
                subscribedEvents = events.filter { it.first.enabled }.map { it.second },
            )
        }
    }

    private suspend fun loadShops(shopIds: Set<String>) =
        if (shopIds.isEmpty()) {
            RamenShops(emptyMap())
        } else {
            (ramenShopRepository.fetchRamenShopsByIds(shopIds) as? RamapResult.Success)
                ?.data
                ?: RamenShops(emptyMap())
        }

    private suspend fun removeShop(shopId: String) {
        if (notificationRepository.updateShopNotification(shopId, false) !is RamapResult.Success) return
        reduce {
            copy(
                shops = shops.without(shopId),
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
        val previous = currentState.areEnabled
        reduce { copy(areEnabled = enabled) }
        if (notificationRepository.updateEnabled(enabled) is RamapResult.Error) {
            reduce { copy(areEnabled = previous) }
        }
    }
}
