package com.peto.ramap.ui.subscribed

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notification.EventNotificationOverride
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.RamenShopRepository
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
                .map { it.notificationShopIds }
                .distinctUntilChanged()
                .collectLatest(::syncSubscribedShops)
        }
        launchTask(
            taskKey = LOAD_EVENTS_TASK_KEY,
            policy = TaskPolicy.CancelPrevious,
        ) {
            fetchSubscribedEvents()
        }
    }

    override suspend fun handleIntent(intent: SubscribedShopListIntent) {
        when (intent) {
            is OnRemovalConfirmed -> {
                val targetId =
                    when (val target = intent.target) {
                        is SubscribedRemovalTarget.Shop -> target.shopId
                        is SubscribedRemovalTarget.EventOverride -> target.eventId
                    }
                confirmRemoval(intent.target)
            }
        }
    }

    private fun syncSubscribedShops(shopIds: Set<String>) {
        if (shopIds.isEmpty()) {
            cancelTask(FETCH_SUBSCRIBED_SHOPS_TASK_KEY)
            reduce { copy(shops = shops.filterByShopIds(shopIds), showError = false) }
            return
        }

        if (currentState.shops.containsAll(shopIds)) {
            cancelTask(FETCH_SUBSCRIBED_SHOPS_TASK_KEY)
            reduce { copy(shops = shops.filterByShopIds(shopIds), showError = false) }
            return
        }

        fetchSubscribedShops(shopIds)
    }

    private fun fetchSubscribedShops(shopIds: Set<String>) {
        launchResultTask(
            taskKey = FETCH_SUBSCRIBED_SHOPS_TASK_KEY,
            loadKey = SubscribedShopLoadKey.FETCH,
            onStart = { copy(showError = false) },
            request = { ramenShopRepository.fetchRamenShops(shopIds) },
            onSuccess = { shops ->
                reduce { copy(shops = shops.filterByShopIds(shopIds), showError = false) }
            },
            onError = { reduce { copy(showError = true) } },
        )
    }

    private suspend fun fetchSubscribedEvents() {
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

    private fun confirmRemoval(target: SubscribedRemovalTarget) {
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
