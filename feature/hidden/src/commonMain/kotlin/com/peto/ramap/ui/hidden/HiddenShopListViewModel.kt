package com.peto.ramap.ui.hidden

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.hidden.contract.HiddenShopListIntent
import com.peto.ramap.ui.hidden.contract.HiddenShopListSideEffect
import com.peto.ramap.ui.hidden.contract.HiddenShopListUiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
                .collectLatest(::fetchHiddenShops)
        }
    }

    override suspend fun handleIntent(intent: HiddenShopListIntent) {
        when (intent) {
            is HiddenShopListIntent.OnUnhideConfirmed -> unhideShop(intent.shopId)
        }
    }

    private suspend fun unhideShop(shopId: String) {
        handleResult(
            result = personalizationStore.unhideShop(shopId),
            onError = {
                trySideEffect(
                    HiddenShopListSideEffect.ShowToast(
                        ToastData(Res.string.personalization_update_failure_message, ToastType.ERROR),
                    ),
                )
            },
        )
    }

    private suspend fun fetchHiddenShops(hiddenShopIds: Set<String>) {
        reduce { copy(shopsState = LoadState.Loading) }
        if (hiddenShopIds.isEmpty()) {
            reduce { copy(shopsState = LoadState.Content(RamenShops(emptyMap()))) }
            return
        }

        handleResult(
            result = ramenShopRepository.fetchRamenShops(hiddenShopIds),
            onSuccess = { shops ->
                reduce {
                    copy(
                        shopsState =
                            LoadState.Content(
                                shops.filterByShopIds(hiddenShopIds).markHidden(hiddenShopIds),
                            ),
                    )
                }
            },
            onError = { reduce { copy(shopsState = LoadState.Error) } },
        )
    }
}
