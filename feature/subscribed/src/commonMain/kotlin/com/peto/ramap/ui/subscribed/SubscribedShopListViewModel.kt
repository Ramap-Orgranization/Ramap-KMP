package com.peto.ramap.ui.subscribed

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notification.EventNotificationOverride
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent.OnRemovalConfirmed
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent.OnRemovalDismissed
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent.OnRemovalRequested
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent.OnRetry
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListSideEffect
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListUiState
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.personalization_update_failure_message

class SubscribedShopListViewModel(
    private val notificationRepository: NotificationSettingsRepository,
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<SubscribedShopListUiState, SubscribedShopListIntent, SubscribedShopListSideEffect>(
        SubscribedShopListUiState(),
    ) {
    init {
        viewModelScope.launch { loadSubscriptions() }
    }

    override suspend fun handleIntent(intent: SubscribedShopListIntent) {
        when (intent) {
            OnRetry -> loadSubscriptions()
            is OnRemovalRequested -> reduce { copy(pendingRemoval = intent.target) }
            OnRemovalDismissed -> reduce { copy(pendingRemoval = null) }
            OnRemovalConfirmed -> confirmRemoval()
        }
    }

    private suspend fun loadSubscriptions() {
        reduce { copy(shopsState = LoadState.Loading, subscribedEvents = emptyList()) }
        handleResult(
            result = fetchInitialSubscriptions(),
            onSuccess = { (shopIds, eventOverrides) ->
                loadSubscriptionDetails(shopIds, eventOverrides)
            },
            onError = { updateLoadErrorState() },
        )
    }

    private suspend fun fetchInitialSubscriptions(): RamapResult<Pair<Set<String>, List<EventNotificationOverride>>> =
        coroutineScope {
            val shopIdsDeferred = async { notificationRepository.fetchSubscribedShopIds() }
            val eventOverridesDeferred = async { notificationRepository.fetchEventOverrides() }
            combineInitialSubscriptions(
                shopIdsDeferred.await(),
                eventOverridesDeferred.await(),
            )
        }

    private fun combineInitialSubscriptions(
        shopIdsResult: RamapResult<Set<String>>,
        eventOverridesResult: RamapResult<List<EventNotificationOverride>>,
    ): RamapResult<Pair<Set<String>, List<EventNotificationOverride>>> {
        if (shopIdsResult is RamapResult.Error) return shopIdsResult
        if (eventOverridesResult is RamapResult.Error) return eventOverridesResult

        return RamapResult.Success(
            (shopIdsResult as RamapResult.Success).data to
                (eventOverridesResult as RamapResult.Success).data,
        )
    }

    private suspend fun loadSubscriptionDetails(
        shopIds: Set<String>,
        eventOverrides: List<EventNotificationOverride>,
    ) {
        handleResult(
            result = fetchSubscriptionDetails(shopIds, eventOverrides),
            onSuccess = { (shops, events) -> updateSubscriptions(shops, events) },
            onError = { updateLoadErrorState() },
        )
    }

    private suspend fun fetchSubscriptionDetails(
        shopIds: Set<String>,
        eventOverrides: List<EventNotificationOverride>,
    ): RamapResult<Pair<RamenShops, List<ShopEvent>>> =
        coroutineScope {
            val shopsDeferred = async { fetchShops(shopIds) }
            val eventDeferreds =
                eventOverrides.map { override ->
                    async { override to ramenShopRepository.fetchActiveEvent(override.eventId) }
                }
            combineSubscriptionDetails(shopsDeferred.await(), eventDeferreds.awaitAll())
        }

    private fun combineSubscriptionDetails(
        shopsResult: RamapResult<RamenShops>,
        eventResults: List<Pair<EventNotificationOverride, RamapResult<ShopEvent?>>>,
    ): RamapResult<Pair<RamenShops, List<ShopEvent>>> {
        if (shopsResult is RamapResult.Error) return shopsResult
        eventResults.forEach { (_, result) ->
            if (result is RamapResult.Error) return result
        }

        val events =
            eventResults.mapNotNull { (override, result) ->
                (result as RamapResult.Success).data?.takeIf { override.enabled }
            }
        return RamapResult.Success((shopsResult as RamapResult.Success).data to events)
    }

    private suspend fun fetchShops(shopIds: Set<String>): RamapResult<RamenShops> =
        if (shopIds.isEmpty()) {
            RamapResult.Success(RamenShops(emptyMap()))
        } else {
            ramenShopRepository.fetchRamenShopsByIds(shopIds)
        }

    private fun updateSubscriptions(
        shops: RamenShops,
        events: List<ShopEvent>,
    ) {
        reduce {
            copy(
                shopsState = LoadState.Content(shops.sortedByName()),
                subscribedEvents = events,
            )
        }
    }

    private fun updateLoadErrorState() {
        reduce { copy(shopsState = LoadState.Error, subscribedEvents = emptyList()) }
    }

    private suspend fun confirmRemoval() {
        val target = currentState.pendingRemoval ?: return
        val result =
            when (target) {
                is SubscribedRemovalTarget.Shop ->
                    notificationRepository.updateShopNotification(target.shopId, false)
                is SubscribedRemovalTarget.EventOverride ->
                    notificationRepository.clearEventNotificationOverride(target.eventId)
            }
        when (result) {
            is RamapResult.Success -> {
                removeTargetFromState(target)
            }
            is RamapResult.Error -> {
                handleRemovalFailure()
            }
        }
    }

    private fun removeTargetFromState(target: SubscribedRemovalTarget) {
        reduce {
            when (target) {
                is SubscribedRemovalTarget.Shop ->
                    copy(
                        shopsState =
                            (shopsState as? LoadState.Content)
                                ?.data
                                ?.without(target.shopId)
                                ?.let { LoadState.Content(it) }
                                ?: shopsState,
                        pendingRemoval = null,
                    )
                is SubscribedRemovalTarget.EventOverride ->
                    copy(
                        subscribedEvents = subscribedEvents.filterNot { it.id == target.eventId },
                        pendingRemoval = null,
                    )
            }
        }
    }

    private suspend fun handleRemovalFailure() {
        reduce { copy(pendingRemoval = null) }
        trySideEffect(
            SubscribedShopListSideEffect.ShowToast(
                ToastData(Res.string.personalization_update_failure_message, ToastType.ERROR),
            ),
        )
    }
}
