package com.peto.ramap.ui.bookmark

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsEvents
import com.peto.ramap.analytics.AnalyticsParams
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListIntent
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListSideEffect
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListUiState
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopLoadKey
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmark_removal_success_message
import ramap.shared.generated.resources.personalization_update_failure_message

class BookmarkedShopListViewModel(
    private val personalizationStore: ShopPersonalizationStore,
    private val ramenShopRepository: RamenShopRepository,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel<BookmarkedShopListUiState, BookmarkedShopListIntent, BookmarkedShopListSideEffect>(
        BookmarkedShopListUiState(),
    ) {
    init {
        viewModelScope.launch {
            personalizationStore.state
                .map { it.bookmarkedShopIds }
                .distinctUntilChanged()
                .collectLatest(::syncBookmarkedShops)
        }
    }

    override suspend fun handleIntent(intent: BookmarkedShopListIntent) {
        when (intent) {
            is BookmarkedShopListIntent.OnRemovalConfirmed -> {
                analyticsTracker.logEvent(
                    AnalyticsEvents.BOOKMARKED_SHOP_REMOVE,
                    mapOf(AnalyticsParams.SHOP_ID to intent.shopId),
                )
                removeBookmark(intent.shopId)
            }
        }
    }

    private fun syncBookmarkedShops(shopIds: Set<String>) {
        if (shopIds.isEmpty()) {
            cancelTask(FETCH_BOOKMARKS_TASK_KEY)
            reduce { copy(shops = shops.filterByShopIds(shopIds), showError = false) }
            return
        }

        if (currentState.shops.containsAll(shopIds)) {
            cancelTask(FETCH_BOOKMARKS_TASK_KEY)
            reduce { copy(shops = shops.filterByShopIds(shopIds), showError = false) }
            return
        }

        fetchBookmarkedShops(shopIds)
    }

    private fun fetchBookmarkedShops(shopIds: Set<String>) {
        launchResultTask(
            taskKey = FETCH_BOOKMARKS_TASK_KEY,
            loadKey = BookmarkedShopLoadKey.FETCH,
            onStart = { copy(showError = false) },
            request = { ramenShopRepository.fetchRamenShops(shopIds) },
            onSuccess = { shops ->
                reduce { copy(shops = shops.filterByShopIds(shopIds), showError = false) }
            },
            onError = { reduce { copy(showError = true) } },
        )
    }

    private fun removeBookmark(shopId: String) {
        launchResultTask(
            taskKey = REMOVE_BOOKMARK_TASK_KEY,
            loadKey = BookmarkedShopLoadKey.REMOVE,
            policy = TaskPolicy.IgnoreNew,
            request = { personalizationStore.updateBookmark(shopId, false) },
            onSuccess = {
                reduce { copy(shops = shops.remove(shopId)) }
                showToast(Res.string.bookmark_removal_success_message, ToastType.SUCCESS)
            },
            onError = { showToast(Res.string.personalization_update_failure_message) },
        )
    }

    private suspend fun showToast(
        message: StringResource,
        type: ToastType = ToastType.ERROR,
    ) {
        postSideEffect(BookmarkedShopListSideEffect.ShowToast(ToastData(message, type)))
    }

    companion object {
        private const val FETCH_BOOKMARKS_TASK_KEY = "fetch-bookmarks"
        private const val REMOVE_BOOKMARK_TASK_KEY = "remove-bookmark"
    }
}
