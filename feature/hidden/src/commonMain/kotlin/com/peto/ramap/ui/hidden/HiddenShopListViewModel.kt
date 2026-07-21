package com.peto.ramap.ui.hidden

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.RamenShopRepository
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
) : BaseViewModel<HiddenShopListUiState, HiddenShopListIntent, HiddenShopListSideEffect>(
        initialState = HiddenShopListUiState(),
    ) {
    init {
        viewModelScope.launch {
            personalizationStore.state
                .map { it.hiddenShopIds }
                .distinctUntilChanged()
                .collectLatest(::syncHiddenShops)
        }
    }

    override suspend fun handleIntent(intent: HiddenShopListIntent) {
        when (intent) {
            is HiddenShopListIntent.OnUnhideConfirmed -> unhideShop(intent.shopId)
        }
    }

    private fun syncHiddenShops(hiddenShopIds: Set<String>) {
        if (hiddenShopIds.isEmpty()) {
            cancelTask(FETCH_SHOP_TASK_KEY)
            reduce { copy(shops = shops.filterByShopIds(hiddenShopIds)) }
            return
        }

        if (currentState.shops.containsAll(hiddenShopIds)) {
            cancelTask(FETCH_SHOP_TASK_KEY)
            reduce { copy(shops = shops.filterByShopIds(hiddenShopIds).markHidden(hiddenShopIds)) }
            return
        }

        fetchHiddenShops(hiddenShopIds)
    }

    private fun fetchHiddenShops(hiddenShopIds: Set<String>) {
        launchResultTask(
            taskKey = FETCH_SHOP_TASK_KEY,
            loadKey = HiddenShopLoadKey.FETCH,
            request = { ramenShopRepository.fetchRamenShops(hiddenShopIds) },
            onSuccess = { shops ->
                reduce { copy(shops = shops.filterByShopIds(hiddenShopIds).markHidden(hiddenShopIds)) }
            },
            onError = { reduce { copy(showError = true) } },
        )
    }

    private fun unhideShop(shopId: String) {
        launchResultTask(
            taskKey = UNHIDE_SHOP_TASK_KEY,
            loadKey = HiddenShopLoadKey.UNHIDE,
            policy = TaskPolicy.IgnoreNew,
            request = { personalizationStore.unhideShop(shopId) },
            onSuccess = {
                reduce { copy(shops = shops.remove(shopId)) }
            },
            onError = {
                showToast(Res.string.personalization_update_failure_message)
            },
        )
    }

    private suspend fun showToast(
        message: StringResource,
        type: ToastType = ToastType.ERROR,
    ) {
        postSideEffect(
            HiddenShopListSideEffect.ShowToast(
                ToastData(message, type),
            ),
        )
    }

    companion object {
        private const val FETCH_SHOP_TASK_KEY = "fetch-hidden-shop"
        private const val UNHIDE_SHOP_TASK_KEY = "unhide-hidden-shop"
    }
}
