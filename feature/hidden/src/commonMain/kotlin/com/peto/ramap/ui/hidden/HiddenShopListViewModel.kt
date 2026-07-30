package com.peto.ramap.ui.hidden

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.hidden.contract.HiddenShopListIntent
import com.peto.ramap.ui.hidden.contract.HiddenShopListSideEffect
import com.peto.ramap.ui.hidden.contract.HiddenShopListUiState
import com.peto.ramap.ui.hidden.contract.HiddenShopLoadKey
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.personalization_update_failure_message

class HiddenShopListViewModel(
    private val personalizationStore: ShopPersonalizationStore,
    private val ramenShopRepository: RamenShopRepository,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel<
        HiddenShopListUiState,
        HiddenShopListIntent,
        HiddenShopListSideEffect,
    >(
        initialState = HiddenShopListUiState(),
    ) {
    init {
        observeHiddenShopIds()
    }

    override suspend fun handleIntent(intent: HiddenShopListIntent) {
        when (intent) {
            HiddenShopListIntent.OnRetry -> retryCurrentHiddenShops()

            is HiddenShopListIntent.OnUnhideConfirmed -> {
                handleUnhideConfirmed(intent)
            }
        }
    }

    private fun retryCurrentHiddenShops() {
        syncHiddenShops(currentHiddenShopIds(), forceFetch = true)
    }

    private fun handleUnhideConfirmed(intent: HiddenShopListIntent.OnUnhideConfirmed) {
        unhideShop(intent.shopId)
    }

    private fun observeHiddenShopIds() {
        viewModelScope.launch {
            personalizationStore.state
                .map { state ->
                    (state as? PersonalizationBootstrapState.Success)?.value?.hiddenShopIds
                }.distinctUntilChanged()
                .collectLatest { hiddenShopIds ->
                    if (hiddenShopIds == null) return@collectLatest
                    syncHiddenShops(hiddenShopIds)
                }
        }
    }

    private fun currentHiddenShopIds(): Set<String> =
        (personalizationStore.state.value as? PersonalizationBootstrapState.Success)
            ?.value
            ?.hiddenShopIds
            ?: emptySet()

    private fun syncHiddenShops(
        hiddenShopIds: Set<String>,
        forceFetch: Boolean = false,
    ) {
        if (hiddenShopIds.isEmpty()) {
            applyExistingHiddenShops(hiddenShopIds)
            return
        }

        if (!forceFetch && currentState.shops.containsAll(hiddenShopIds)) {
            applyExistingHiddenShops(hiddenShopIds)
            return
        }

        fetchHiddenShops(hiddenShopIds)
    }

    private fun applyExistingHiddenShops(hiddenShopIds: Set<String>) {
        cancelTask(FETCH_SHOP_TASK_KEY)

        val hiddenShops =
            createHiddenShops(
                shops = currentState.shops,
                hiddenShopIds = hiddenShopIds,
            )

        reduce {
            copy(
                shops = hiddenShops,
                showError = false,
                hasLoaded = true,
            )
        }
    }

    private fun fetchHiddenShops(hiddenShopIds: Set<String>) {
        launchResultTask(
            taskKey = FETCH_SHOP_TASK_KEY,
            loadKey = HiddenShopLoadKey.FETCH,
            onStart = { copy(showError = false) },
            request = { ramenShopRepository.fetchRamenShops(hiddenShopIds) },
            onSuccess = { shops -> handleFetchSuccess(shops, hiddenShopIds) },
            onError = {
                reduce { copy(showError = true, hasLoaded = true) }
            },
        )
    }

    private fun handleFetchSuccess(
        shops: RamenShops,
        requestedShopIds: Set<String>,
    ) {
        val hiddenShops = createHiddenShops(shops, requestedShopIds)
        reduce { copy(shops = hiddenShops, showError = false, hasLoaded = true) }
    }

    private fun createHiddenShops(
        shops: RamenShops,
        hiddenShopIds: Set<String>,
    ): RamenShops {
        val filteredShops = shops.filterByShopIds(hiddenShopIds)

        return filteredShops.markHidden(hiddenShopIds)
    }

    private fun unhideShop(shopId: String) {
        launchResultTask(
            taskKey = UNHIDE_SHOP_TASK_KEY,
            loadKey = HiddenShopLoadKey.UNHIDE,
            policy = TaskPolicy.IgnoreNew,
            request = {
                personalizationStore.unhideShop(shopId)
            },
            onSuccess = {
                handleUnhideSuccess(shopId)
            },
            onError = {
                showToast(
                    Res.string.personalization_update_failure_message,
                    ToastType.ERROR,
                )
            },
        )
    }

    private fun handleUnhideSuccess(shopId: String) {
        reduce {
            copy(
                shops = shops.remove(shopId),
            )
        }
    }

    private suspend fun showToast(
        message: StringResource,
        type: ToastType,
    ) {
        postSideEffect(
            HiddenShopListSideEffect.ShowToast(
                ToastData(
                    message = message,
                    type = type,
                ),
            ),
        )
    }

    companion object {
        private const val FETCH_SHOP_TASK_KEY = "fetch-hidden-shop"
        private const val UNHIDE_SHOP_TASK_KEY = "unhide-hidden-shop"
    }
}
