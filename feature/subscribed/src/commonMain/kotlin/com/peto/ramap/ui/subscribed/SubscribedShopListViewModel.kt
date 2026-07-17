package com.peto.ramap.ui.subscribed

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
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
        viewModelScope.launch { loadSubscribedShops() }
    }

    override suspend fun handleIntent(intent: SubscribedShopListIntent) {
        when (intent) {
            OnRetry -> loadSubscribedShops()
            is OnRemovalRequested -> reduce { copy(pendingRemoval = intent.target) }
            OnRemovalDismissed -> reduce { copy(pendingRemoval = null) }
            OnRemovalConfirmed -> confirmRemoval()
        }
    }

    private suspend fun loadSubscribedShops() {
        reduce { copy(shopsState = LoadState.Loading) }
        handleResult(
            result = notificationRepository.fetchSubscribedShopIds(),
            onSuccess = ::loadShopDetails,
            onError = { reduce { copy(shopsState = LoadState.Error) } },
        )
    }

    private suspend fun loadShopDetails(shopIds: Set<String>) {
        if (shopIds.isEmpty()) {
            reduce { copy(shopsState = LoadState.Content(RamenShops(emptyMap()))) }
            return
        }

        handleResult(
            result = ramenShopRepository.fetchRamenShopsByIds(shopIds),
            onSuccess = { shops ->
                reduce { copy(shopsState = LoadState.Content(shops.sortedByName())) }
            },
            onError = { reduce { copy(shopsState = LoadState.Error) } },
        )
    }

    private suspend fun confirmRemoval() {
        val target = currentState.pendingRemoval as? SubscribedRemovalTarget.Shop ?: return
        when (notificationRepository.updateShopNotification(target.shopId, false)) {
            is RamapResult.Success -> {
                val currentShops = (currentState.shopsState as? LoadState.Content)?.data
                reduce {
                    copy(
                        shopsState =
                            currentShops
                                ?.without(target.shopId)
                                ?.let { LoadState.Content(it) }
                                ?: shopsState,
                        pendingRemoval = null,
                    )
                }
            }
            is RamapResult.Error -> {
                reduce { copy(pendingRemoval = null) }
                trySideEffect(
                    SubscribedShopListSideEffect.ShowToast(
                        ToastData(Res.string.personalization_update_failure_message, ToastType.ERROR),
                    ),
                )
            }
        }
    }
}
