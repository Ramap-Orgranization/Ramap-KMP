package com.peto.ramap.ui.shop.menu

import com.peto.ramap.domain.repository.ShopMenuRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.shop.menu.contract.ShopMenuIntent
import com.peto.ramap.ui.shop.menu.contract.ShopMenuLoadKey
import com.peto.ramap.ui.shop.menu.contract.ShopMenuSideEffect
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class ShopMenuViewModel(
    private val repository: ShopMenuRepository,
) : BaseViewModel<ShopMenuUiState, ShopMenuIntent, ShopMenuSideEffect>(ShopMenuUiState()) {
    override suspend fun handleIntent(intent: ShopMenuIntent) {
        when (intent) {
            is ShopMenuIntent.OnShopSelected -> {
                if (currentState.shopId == intent.shopId) return
                reduce { copy(shopId = intent.shopId, menu = null, hasLoadFailure = false) }
                fetchMenu()
            }

            ShopMenuIntent.OnRetry -> fetchMenu()
        }
    }

    private fun fetchMenu() {
        val shopId = currentState.shopId ?: return
        launchResultTask(
            taskKey = MENU,
            loadKey = ShopMenuLoadKey.Menu,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(hasLoadFailure = false) },
            request = { repository.fetchShopMenu(shopId) },
            onSuccess = { menu ->
                if (!currentCoroutineContext().isActive || currentState.shopId != shopId) return@launchResultTask
                reduce { copy(menu = menu, hasLoadFailure = false) }
            },
            onError = {
                if (currentCoroutineContext().isActive && currentState.shopId == shopId) reduce { copy(hasLoadFailure = true) }
            },
        )
    }

    companion object {
        private const val MENU = "shop_menu"
    }
}
