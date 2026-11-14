package com.peto.ramap.ui.main.newshops

import com.peto.ramap.domain.model.shop.ShopRegistrationMonth
import com.peto.ramap.domain.repository.NewShopsRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.newshops.contract.NewShopsIntent
import com.peto.ramap.ui.main.newshops.contract.NewShopsLoadKey
import com.peto.ramap.ui.main.newshops.contract.NewShopsSideEffect
import com.peto.ramap.ui.main.newshops.contract.NewShopsUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlin.time.Clock

internal class NewShopsViewModel(
    private val repository: NewShopsRepository,
    private val clock: Clock,
) : BaseViewModel<NewShopsUiState, NewShopsIntent, NewShopsSideEffect>(
        NewShopsUiState(month = ShopRegistrationMonth.containing(clock.now())),
    ) {
    init {
        loadMonth(currentState.month)
    }

    override suspend fun handleIntent(intent: NewShopsIntent) {
        when (intent) {
            is NewShopsIntent.OnShown -> {
                if (intent.month != currentState.month) loadMonth(intent.month)
            }

            is NewShopsIntent.OnRefreshRequested -> loadMonth(intent.month)
        }
    }

    fun currentRegistrationMonth(): ShopRegistrationMonth = ShopRegistrationMonth.containing(clock.now())

    private fun loadMonth(month: ShopRegistrationMonth) {
        val isMonthChange = month != currentState.month
        val hasCurrentList = !isMonthChange && currentState.shops != null
        launchResultTask(
            taskKey = LOAD_SHOPS,
            loadKey = if (hasCurrentList) NewShopsLoadKey.Refresh else NewShopsLoadKey.Fetch,
            policy = if (isMonthChange) TaskPolicy.CancelPrevious else TaskPolicy.IgnoreNew,
            onStart = { copy(month = month, shops = shops.takeUnless { isMonthChange }, failed = false) },
            request = { repository.fetchNewShops(month) },
            onSuccess = { shops -> reduce { copy(shops = shops) } },
            onError = {
                if (hasCurrentList) {
                    trySideEffect(NewShopsSideEffect.ShowRefreshFailure)
                } else {
                    reduce { copy(failed = true) }
                }
            },
        )
    }

    private companion object {
        const val LOAD_SHOPS = "loadNewShops"
    }
}
