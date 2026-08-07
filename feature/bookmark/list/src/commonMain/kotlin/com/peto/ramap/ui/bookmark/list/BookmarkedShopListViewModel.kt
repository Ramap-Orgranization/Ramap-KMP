package com.peto.ramap.ui.bookmark.list

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopListIntent
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopListSideEffect
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopListUiState
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopLoadKey
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
) : BaseViewModel<BookmarkedShopListUiState, BookmarkedShopListIntent, BookmarkedShopListSideEffect>(
        initialState = BookmarkedShopListUiState(),
    ) {
    init {
        observeBookmarkedShopIds()
    }

    override suspend fun handleIntent(intent: BookmarkedShopListIntent) {
        when (intent) {
            BookmarkedShopListIntent.OnRetry -> retryCurrentBookmarks()

            is BookmarkedShopListIntent.OnRemovalConfirmed -> {
                handleRemovalConfirmed(intent)
            }
        }
    }

    private fun retryCurrentBookmarks() {
        syncBookmarkedShops(currentBookmarkedShopIds(), forceFetch = true)
    }

    private fun handleRemovalConfirmed(intent: BookmarkedShopListIntent.OnRemovalConfirmed) {
        removeBookmark(intent.shopId)
    }

    private fun observeBookmarkedShopIds() {
        viewModelScope.launch {
            personalizationStore.state
                .map { state ->
                    (state as? PersonalizationBootstrapState.Success)?.value?.bookmarkedShopIds
                }.distinctUntilChanged()
                .collectLatest { shopIds ->
                    if (shopIds == null) return@collectLatest
                    syncBookmarkedShops(shopIds)
                }
        }
    }

    private fun currentBookmarkedShopIds(): Set<String> =
        (personalizationStore.state.value as? PersonalizationBootstrapState.Success)
            ?.value
            ?.bookmarkedShopIds
            ?: emptySet()

    private fun syncBookmarkedShops(
        shopIds: Set<String>,
        forceFetch: Boolean = false,
    ) {
        if (shopIds.isEmpty()) {
            applyExistingBookmarkedShops(shopIds)
            return
        }

        if (!forceFetch && currentState.shops.containsAll(shopIds)) {
            applyExistingBookmarkedShops(shopIds)
            return
        }

        fetchBookmarkedShops(shopIds)
    }

    private fun applyExistingBookmarkedShops(shopIds: Set<String>) {
        cancelTask(FETCH_BOOKMARKS_TASK_KEY)

        reduce {
            copy(
                shops = shops.filterByShopIds(shopIds),
                showError = false,
                hasLoaded = true,
            )
        }
    }

    private fun fetchBookmarkedShops(shopIds: Set<String>) {
        launchResultTask(
            taskKey = FETCH_BOOKMARKS_TASK_KEY,
            loadKey = BookmarkedShopLoadKey.FETCH,
            onStart = { copy(showError = false) },
            request = { ramenShopRepository.fetchRamenShops(shopIds) },
            onSuccess = { shops -> handleFetchSuccess(shops, shopIds) },
            onError = { reduce { copy(showError = true, hasLoaded = true) } },
        )
    }

    private fun handleFetchSuccess(
        shops: RamenShops,
        requestedShopIds: Set<String>,
    ) {
        reduce {
            copy(
                shops =
                    shops.filterByShopIds(
                        requestedShopIds,
                    ),
                showError = false,
                hasLoaded = true,
            )
        }
    }

    private fun removeBookmark(shopId: String) {
        launchResultTask(
            taskKey = REMOVE_BOOKMARK_TASK_KEY,
            loadKey = BookmarkedShopLoadKey.REMOVE,
            policy = TaskPolicy.IgnoreNew,
            request = { personalizationStore.updateBookmark(shopId, false) },
            onSuccess = { handleBookmarkRemovalSuccess(shopId) },
            onError = {
                showToast(
                    Res.string.personalization_update_failure_message,
                    type = ToastType.ERROR,
                )
            },
        )
    }

    private fun handleBookmarkRemovalSuccess(shopId: String) {
        reduce { copy(shops = shops.remove(shopId)) }
        showToast(Res.string.bookmark_removal_success_message, ToastType.SUCCESS)
    }

    private fun showToast(
        message: StringResource,
        type: ToastType,
    ) {
        viewModelScope.launch {
            postSideEffect(
                BookmarkedShopListSideEffect.ShowToast(ToastData(message, type)),
            )
        }
    }

    companion object {
        private const val FETCH_BOOKMARKS_TASK_KEY = "fetch-bookmarks"

        private const val REMOVE_BOOKMARK_TASK_KEY = "remove-bookmark"
    }
}
