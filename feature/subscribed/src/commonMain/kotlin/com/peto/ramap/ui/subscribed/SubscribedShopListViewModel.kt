package com.peto.ramap.ui.subscribed

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.common.event.EventNotificationToggled
import com.peto.ramap.analytics.common.shop.SubscribedToggled
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notification.EventNotificationOverride
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent.OnRemovalConfirmed
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListSideEffect
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListUiState
import com.peto.ramap.ui.subscribed.contract.SubscribedShopLoadKey
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget
import com.peto.ramap.ui.task.TaskPolicy
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
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel<SubscribedShopListUiState, SubscribedShopListIntent, SubscribedShopListSideEffect>(
        initialState = SubscribedShopListUiState(),
    ) {
    init {
        viewModelScope.launch {
            personalizationStore.state
                .map { (it as? PersonalizationBootstrapState.Success)?.value?.notificationShopIds }
                .distinctUntilChanged()
                .collectLatest { shopIds ->
                    if (shopIds == null) return@collectLatest
                    syncSubscribedShops(shopIds)
                }
        }
        loadSubscribedEvents()
    }

    override suspend fun handleIntent(intent: SubscribedShopListIntent) {
        when (intent) {
            SubscribedShopListIntent.OnRetry -> retryCurrentSubscriptions()

            is OnRemovalConfirmed -> {
                confirmRemoval(intent.target)
            }
        }
    }

    private fun retryCurrentSubscriptions() {
        syncSubscribedShops(
            shopIds = currentNotificationShopIds(),
            forceFetch = true,
        )
        loadSubscribedEvents()
    }

    private fun currentNotificationShopIds(): Set<String> =
        (personalizationStore.state.value as? PersonalizationBootstrapState.Success)
            ?.value
            ?.notificationShopIds
            ?: emptySet()

    private fun syncSubscribedShops(
        shopIds: Set<String>,
        forceFetch: Boolean = false,
    ) {
        if (shopIds.isEmpty()) {
            cancelTask(FETCH_SUBSCRIBED_SHOPS_TASK_KEY)
            reduce {
                copy(
                    shops = shops.filterByShopIds(shopIds),
                    showShopError = false,
                    haveShopsLoaded = true,
                )
            }
            return
        }

        if (!forceFetch && currentState.shops.containsAll(shopIds)) {
            cancelTask(FETCH_SUBSCRIBED_SHOPS_TASK_KEY)
            reduce {
                copy(
                    shops = shops.filterByShopIds(shopIds),
                    showShopError = false,
                    haveShopsLoaded = true,
                )
            }
            return
        }

        fetchSubscribedShops(shopIds)
    }

    private fun fetchSubscribedShops(shopIds: Set<String>) {
        launchResultTask(
            taskKey = FETCH_SUBSCRIBED_SHOPS_TASK_KEY,
            loadKey = SubscribedShopLoadKey.SHOPS,
            onStart = { copy(showShopError = false) },
            request = { ramenShopRepository.fetchRamenShops(shopIds) },
            onSuccess = { shops ->
                reduce {
                    copy(
                        shops = shops.filterByShopIds(shopIds),
                        showShopError = false,
                        haveShopsLoaded = true,
                    )
                }
            },
            onError = {
                reduce {
                    copy(
                        showShopError = true,
                        haveShopsLoaded = true,
                    )
                }
            },
        )
    }

    private fun loadSubscribedEvents() {
        launchResultTask(
            taskKey = LOAD_EVENTS_TASK_KEY,
            loadKey = SubscribedShopLoadKey.EVENTS,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(showEventError = false) },
            request = { fetchSubscribedEvents() },
            onSuccess = { events ->
                reduce {
                    copy(
                        subscribedEvents = events,
                        showEventError = false,
                        haveEventsLoaded = true,
                    )
                }
            },
            onError = {
                reduce {
                    copy(
                        showEventError = true,
                        haveEventsLoaded = true,
                    )
                }
            },
        )
    }

    private suspend fun fetchSubscribedEvents(): RamapResult<List<ShopEvent>> {
        val overrides = notificationRepository.fetchEventOverrides()
        if (overrides is RamapResult.Error) return overrides

        return fetchEnabledActiveEvents((overrides as RamapResult.Success).data)
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

    private fun confirmRemoval(target: SubscribedRemovalTarget) {
        logRemoval(target)
        launchResultTask(
            taskKey = REMOVE_SUBSCRIPTION_TASK_KEY,
            loadKey = SubscribedShopLoadKey.REMOVE,
            policy = TaskPolicy.IgnoreNew,
            request = {
                when (target) {
                    is SubscribedRemovalTarget.Shop ->
                        personalizationStore.updateShopNotification(target.shopId, false)

                    is SubscribedRemovalTarget.EventOverride ->
                        notificationRepository.clearEventNotificationOverride(target.eventId)
                }
            },
            onSuccess = { removeTargetFromState(target) },
            onError = { handleRemovalFailure() },
        )
    }

    private fun logRemoval(target: SubscribedRemovalTarget) {
        when (target) {
            is SubscribedRemovalTarget.Shop -> {
                currentState.shops[target.shopId]?.let { shop ->
                    analyticsTracker.logEvent(
                        SubscribedToggled(
                            shopId = shop.id,
                            shopName = shop.name,
                            enabled = false,
                            source = AnalyticsSource.SUBSCRIBED_SHOPS,
                        ),
                    )
                }
            }

            is SubscribedRemovalTarget.EventOverride ->
                analyticsTracker.logEvent(
                    EventNotificationToggled(
                        eventId = target.eventId,
                        enabled = false,
                        source = AnalyticsSource.SUBSCRIBED_SHOPS,
                    ),
                )
        }
    }

    private fun removeTargetFromState(target: SubscribedRemovalTarget) {
        reduce {
            when (target) {
                is SubscribedRemovalTarget.Shop ->
                    copy(shops = shops.remove(target.shopId))

                is SubscribedRemovalTarget.EventOverride ->
                    copy(subscribedEvents = subscribedEvents.filterNot { it.id == target.eventId })
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

    companion object {
        private const val FETCH_SUBSCRIBED_SHOPS_TASK_KEY = "fetch-subscribed-shops"
        private const val LOAD_EVENTS_TASK_KEY = "load-subscription-events"
        private const val REMOVE_SUBSCRIPTION_TASK_KEY = "remove-subscription"
    }
}
