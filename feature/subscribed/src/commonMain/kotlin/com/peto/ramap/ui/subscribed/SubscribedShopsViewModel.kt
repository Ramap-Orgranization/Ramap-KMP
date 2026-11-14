package com.peto.ramap.ui.subscribed

import androidx.lifecycle.viewModelScope
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.base.SideEffect
import com.peto.ramap.ui.subscribed.contract.SubscribedShopsIntent
import com.peto.ramap.ui.subscribed.contract.SubscribedShopsLoadKey
import com.peto.ramap.ui.subscribed.contract.SubscribedShopsUiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SubscribedShopsViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val personalizationStore: PersonalizationStore,
) : BaseViewModel<SubscribedShopsUiState, SubscribedShopsIntent, SideEffect>(
        initialState = SubscribedShopsUiState(),
    ) {
    init {
        viewModelScope.launch {
            personalizationStore.state
                .map { (it as? PersonalizationBootstrapState.Success)?.value?.notificationShopIds }
                .distinctUntilChanged()
                .collectLatest { shopIds ->
                    val isShopIdsNull = shopIds == null
                    if (isShopIdsNull) return@collectLatest
                    syncSubscribedShops(shopIds)
                }
        }
    }

    override suspend fun handleIntent(intent: SubscribedShopsIntent) {
        when (intent) {
            SubscribedShopsIntent.OnRetry -> retryCurrentSubscriptions()
        }
    }

    private fun retryCurrentSubscriptions() {
        syncSubscribedShops(
            shopIds = currentNotificationShopIds(),
            forceFetch = true,
        )
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
        val isShopIdsEmpty = shopIds.isEmpty()
        if (isShopIdsEmpty) {
            cancelTask(FETCH_SUBSCRIBED_SHOPS_TASK_KEY)
            reduce {
                copy(
                    shops = shops.filterByShopIds(shopIds),
                    showError = false,
                    haveShopsLoaded = true,
                )
            }
            return
        }

        val isAlreadyCached = !forceFetch && currentState.shops.containsAll(shopIds)
        if (isAlreadyCached) {
            cancelTask(FETCH_SUBSCRIBED_SHOPS_TASK_KEY)
            reduce {
                copy(
                    shops = shops.filterByShopIds(shopIds),
                    showError = false,
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
            loadKey = SubscribedShopsLoadKey.SHOPS,
            onStart = { copy(showError = false) },
            request = { ramenShopRepository.fetchRamenShops(shopIds) },
            onSuccess = { shops ->
                reduce {
                    copy(
                        shops = shops.filterByShopIds(shopIds),
                        showError = false,
                        haveShopsLoaded = true,
                    )
                }
            },
            onError = {
                reduce {
                    copy(
                        showError = true,
                        haveShopsLoaded = true,
                    )
                }
            },
        )
    }

    companion object {
        private const val FETCH_SUBSCRIBED_SHOPS_TASK_KEY = "fetch-subscribed-shops"
    }
}
