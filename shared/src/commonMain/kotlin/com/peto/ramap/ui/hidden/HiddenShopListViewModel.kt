package com.peto.ramap.ui.hidden

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.base.BaseViewModel
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.hidden.contract.HiddenShopListIntent
import com.peto.ramap.ui.hidden.contract.HiddenShopListSideEffect
import com.peto.ramap.ui.hidden.contract.HiddenShopListUiState
import com.peto.ramap.ui.hidden.contract.OnHiddenShopListRetried
import kotlinx.coroutines.launch

class HiddenShopListViewModel(
    private val personalizationRepository: PersonalizationRepository,
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<HiddenShopListUiState, HiddenShopListIntent, HiddenShopListSideEffect>(
        initialState = HiddenShopListUiState(),
    ) {
    init {
        viewModelScope.launch { loadHiddenShops() }
    }

    override suspend fun handleIntent(intent: HiddenShopListIntent) {
        when (intent) {
            OnHiddenShopListRetried -> loadHiddenShops()
        }
    }

    private suspend fun loadHiddenShops() {
        reduce { copy(shopsState = LoadState.Loading) }

        val personalizationResult = personalizationRepository.fetchPersonalization()
        handleResult(
            result = personalizationResult,
            onSuccess = { personalization ->
                val hiddenShopIds = personalization.hiddenShopIds
                if (hiddenShopIds.isEmpty()) {
                    reduce { copy(shopsState = LoadState.Content(emptyList())) }
                    return@handleResult
                }

                handleResult(
                    result = ramenShopRepository.fetchRamenShopsByIds(hiddenShopIds),
                    onSuccess = { shops ->
                        reduce {
                            copy(
                                shopsState = LoadState.Content(shops.values.map { it.copy(isVisible = false) }),
                            )
                        }
                    },
                    onError = {
                        reduce { copy(shopsState = LoadState.Error) }
                    },
                )
            },
            onError = {
                reduce { copy(shopsState = LoadState.Error) }
            },
        )
    }
}
