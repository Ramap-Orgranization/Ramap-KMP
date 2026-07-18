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
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent.OnRemovalConfirmed
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListSideEffect
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListUiState
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.personalization_update_failure_message

class SubscribedShopListViewModel(
    private val notificationRepository: NotificationSettingsRepository,
    private val ramenShopRepository: RamenShopRepository,
    private val personalizationStore: ShopPersonalizationStore,
) : BaseViewModel<SubscribedShopListUiState, SubscribedShopListIntent, SubscribedShopListSideEffect>(
        SubscribedShopListUiState(),
    ) {
    init {
        viewModelScope.launch {
            personalizationStore.state
                .map { it.notificationShopIds }
                .distinctUntilChanged()
                .collectLatest(::fetchSubscribedShops)
        }
        viewModelScope.launch { loadSubscribedEvents() }
    }

    override suspend fun handleIntent(intent: SubscribedShopListIntent) {
        when (intent) {
            is OnRemovalConfirmed -> confirmRemoval(intent.target)
        }
    }

    private suspend fun fetchSubscribedShops(shopIds: Set<String>) {
        reduce { copy(shopsState = LoadState.Loading) }
        if (shopIds.isEmpty()) {
            reduce { copy(shopsState = LoadState.Content(RamenShops(emptyMap()))) }
            return
        }

        handleResult(
            result = ramenShopRepository.fetchRamenShops(shopIds),
            onSuccess = { shops ->
                reduce {
                    copy(shopsState = LoadState.Content(shops.filterByShopIds(shopIds)))
                }
            },
            onError = { reduce { copy(shopsState = LoadState.Error) } },
        )
    }

    private suspend fun loadSubscribedEvents() {
        handleResult(
            result = notificationRepository.fetchEventOverrides(),
            onSuccess = ::fetchActiveEvents,
            onError = { reduce { copy(subscribedEvents = emptyList()) } },
        )
    }

    private suspend fun fetchActiveEvents(eventOverrides: List<EventNotificationOverride>) {
        handleResult(
            result = fetchEnabledActiveEvents(eventOverrides),
            onSuccess = { events -> reduce { copy(subscribedEvents = events) } },
            onError = { reduce { copy(subscribedEvents = emptyList()) } },
        )
    }

    private suspend fun fetchEnabledActiveEvents(eventOverrides: List<EventNotificationOverride>): RamapResult<List<ShopEvent>> =
        coroutineScope {
            val eventDeferreds =
                eventOverrides.map { override ->
                    async { override to ramenShopRepository.fetchActiveEvent(override.eventId) }
                }
            combineActiveEvents(eventDeferreds.awaitAll())
        }

    private fun combineActiveEvents(
        eventResults: List<Pair<EventNotificationOverride, RamapResult<ShopEvent?>>>,
    ): RamapResult<List<ShopEvent>> {
        eventResults.forEach { (_, result) ->
            if (result is RamapResult.Error) return result
        }

        val events =
            eventResults.mapNotNull { (override, result) ->
                (result as RamapResult.Success).data?.takeIf { override.enabled }
            }
        return RamapResult.Success(events)
    }

    private suspend fun confirmRemoval(target: SubscribedRemovalTarget) {
        val result =
            when (target) {
                is SubscribedRemovalTarget.Shop ->
                    personalizationStore.updateShopNotification(target.shopId, false)
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
                    )
                is SubscribedRemovalTarget.EventOverride ->
                    copy(
                        subscribedEvents = subscribedEvents.filterNot { it.id == target.eventId },
                    )
            }
        }
    }

    private fun handleRemovalFailure() {
        trySideEffect(
            SubscribedShopListSideEffect.ShowToast(
                ToastData(Res.string.personalization_update_failure_message, ToastType.ERROR),
            ),
        )
    }
}
