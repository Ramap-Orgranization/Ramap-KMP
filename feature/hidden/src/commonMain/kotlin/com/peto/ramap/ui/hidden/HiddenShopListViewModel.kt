package com.peto.ramap.ui.hidden

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.hidden.contract.HiddenShopListIntent
import com.peto.ramap.ui.hidden.contract.HiddenShopListSideEffect
import com.peto.ramap.ui.hidden.contract.HiddenShopListUiState
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.personalization_update_failure_message

class HiddenShopListViewModel(
    private val personalizationRepository: PersonalizationRepository,
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<HiddenShopListUiState, HiddenShopListIntent, HiddenShopListSideEffect>(
        initialState = HiddenShopListUiState(),
    ) {
    init {
        viewModelScope.launch { fetchHiddenShops() }
    }

    override suspend fun handleIntent(intent: HiddenShopListIntent) {
        when (intent) {
            HiddenShopListIntent.OnHiddenShopListRetried -> fetchHiddenShops()
            is HiddenShopListIntent.OnShopClicked -> reduce { copy(pendingUnhideShopId = intent.shopId) }
            HiddenShopListIntent.OnUnhideDismissed -> reduce { copy(pendingUnhideShopId = null) }
            HiddenShopListIntent.OnUnhideConfirmed -> unhidePendingShop()
        }
    }

    private suspend fun unhidePendingShop() {
        val shopId = currentState.pendingUnhideShopId ?: return
        when (personalizationRepository.unhideShop(shopId)) {
            is RamapResult.Success ->
                reduce {
                    val content = shopsState as? LoadState.Content
                    copy(
                        shopsState = content?.let { LoadState.Content(it.data.without(shopId)) } ?: shopsState,
                        pendingUnhideShopId = null,
                    )
                }
            is RamapResult.Error -> {
                reduce { copy(pendingUnhideShopId = null) }
                trySideEffect(
                    HiddenShopListSideEffect.ShowToast(
                        ToastData(Res.string.personalization_update_failure_message, ToastType.ERROR),
                    ),
                )
            }
        }
    }

    private suspend fun fetchHiddenShops() {
        reduce { copy(shopsState = LoadState.Loading) }

        handleResult(
            result = personalizationRepository.fetchPersonalization(),
            onSuccess = { personalization -> fetchHiddenShops(personalization.hiddenShopIds) },
            onError = { reduce { copy(shopsState = LoadState.Error) } },
        )
    }

    private suspend fun fetchHiddenShops(hiddenShopIds: Set<String>) {
        if (hiddenShopIds.isEmpty()) {
            reduce { copy(shopsState = LoadState.Content(RamenShops(emptyMap()))) }
            return
        }

        handleResult(
            result = ramenShopRepository.fetchRamenShopsByIds(hiddenShopIds),
            onSuccess = { shops ->
                reduce { copy(shopsState = LoadState.Content(shops.markHidden(hiddenShopIds))) }
            },
            onError = { reduce { copy(shopsState = LoadState.Error) } },
        )
    }
}
