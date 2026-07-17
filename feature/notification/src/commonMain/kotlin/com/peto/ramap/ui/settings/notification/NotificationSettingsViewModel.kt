package com.peto.ramap.ui.settings.notification

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notification.EventNotificationOverride
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnEventNotificationsEnabledChanged
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnEventOverrideRemoved
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnNotificationSettingsRetried
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalConfirmed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalDismissed
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnRemovalRequested
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsIntent.OnShopRemoved
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsSideEffect
import com.peto.ramap.ui.settings.notification.contract.NotificationSettingsUiState
import com.peto.ramap.ui.settings.notification.model.NotificationRemovalTarget
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
            OnNotificationSettingsRetried -> loadSettings()
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
        reduce { copy(loadState = LoadState.Loading) }
        handleResult(
            result = fetchInitialSettings(),
            onSuccess = { (enabled, subscribedShopIds, eventOverrides) ->
                loadSubscribedDetails(enabled, subscribedShopIds, eventOverrides)
            },
            onError = { updateLoadErrorState() },
        )
    }

    private suspend fun loadSubscribedDetails(
        enabled: Boolean,
        subscribedShopIds: Set<String>,
        eventOverrides: List<EventNotificationOverride>,
    ) {
        handleResult(
            result = fetchSubscribedDetails(subscribedShopIds, eventOverrides),
            onSuccess = { (shops, events) ->
                updateSettingsState(enabled, subscribedShopIds.size, shops, events)
            },
            onError = { updateLoadErrorState() },
        )
    }

    private suspend fun fetchInitialSettings(): RamapResult<Triple<Boolean, Set<String>, List<EventNotificationOverride>>> =
        coroutineScope {
            val enabledDeferred = async { notificationRepository.fetchEventNotificationsEnabled() }
            val shopIdsDeferred = async { notificationRepository.fetchSubscribedShopIds() }
            val overridesDeferred = async { notificationRepository.fetchEventOverrides() }
            combineInitialSettings(
                enabledResult = enabledDeferred.await(),
                shopIdsResult = shopIdsDeferred.await(),
                overridesResult = overridesDeferred.await(),
            )
        }

    private fun combineInitialSettings(
        enabledResult: RamapResult<Boolean>,
        shopIdsResult: RamapResult<Set<String>>,
        overridesResult: RamapResult<List<EventNotificationOverride>>,
    ): RamapResult<Triple<Boolean, Set<String>, List<EventNotificationOverride>>> {
        if (enabledResult is RamapResult.Error) return enabledResult
        if (shopIdsResult is RamapResult.Error) return shopIdsResult
        if (overridesResult is RamapResult.Error) return overridesResult

        return RamapResult.Success(
            Triple(
                (enabledResult as RamapResult.Success).data,
                (shopIdsResult as RamapResult.Success).data,
                (overridesResult as RamapResult.Success).data,
            ),
        )
    }

    private suspend fun fetchSubscribedDetails(
        subscribedShopIds: Set<String>,
        eventOverrides: List<EventNotificationOverride>,
    ): RamapResult<Pair<RamenShops, List<ShopEvent>>> =
        coroutineScope {
            val shopsDeferred = async { loadShops(subscribedShopIds) }
            val eventDeferreds =
                eventOverrides.map { override ->
                    async { override to ramenShopRepository.fetchActiveEvent(override.eventId) }
                }
            combineSubscribedDetails(
                shopsResult = shopsDeferred.await(),
                eventResults = eventDeferreds.awaitAll(),
            )
        }

    private fun combineSubscribedDetails(
        shopsResult: RamapResult<RamenShops>,
        eventResults: List<Pair<EventNotificationOverride, RamapResult<ShopEvent?>>>,
    ): RamapResult<Pair<RamenShops, List<ShopEvent>>> {
        if (shopsResult is RamapResult.Error) return shopsResult
        for ((_, eventResult) in eventResults) {
            if (eventResult is RamapResult.Error) return eventResult
        }

        val shops = (shopsResult as RamapResult.Success).data
        val events =
            eventResults.mapNotNull { (override, result) ->
                (result as RamapResult.Success).data?.takeIf { override.enabled }
            }
        return RamapResult.Success(shops to events)
    }

    private fun updateLoadErrorState() {
        reduce { copy(loadState = LoadState.Error) }
    }

    private fun updateSettingsState(
        enabled: Boolean,
        subscribedShopCount: Int,
        shops: RamenShops,
        events: List<ShopEvent>,
    ) {
        reduce {
            copy(
                loadState = LoadState.Content(Unit),
                areEnabled = enabled,
                subscribedShopCount = subscribedShopCount,
                shops = shops.sortedByName(),
                subscribedEvents = events,
            )
        }
    }

    private suspend fun loadShops(shopIds: Set<String>): RamapResult<RamenShops> =
        if (shopIds.isEmpty()) {
            RamapResult.Success(RamenShops(emptyMap()))
        } else {
            ramenShopRepository.fetchRamenShopsByIds(shopIds)
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
        if (notificationRepository.updateEventNotificationsEnabled(enabled) is RamapResult.Error) {
            reduce { copy(areEnabled = previous) }
        }
    }
}
