package com.peto.ramap.ui.bookmark.list

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.common.shop.BookmarkToggled
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopListIntent
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopListSideEffect
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopListUiState
import com.peto.ramap.ui.bookmark.list.contract.BookmarkedShopLoadKey
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmark_removal_success_message
import ramap.shared.generated.resources.personalization_update_failure_message

class BookmarkedShopListViewModel(
    private val personalizationStore: PersonalizationStore,
    private val ramenShopRepository: RamenShopRepository,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel<BookmarkedShopListUiState, BookmarkedShopListIntent, BookmarkedShopListSideEffect>(
        initialState = BookmarkedShopListUiState(),
    ) {
    init {
        observeBookmarkedShopIds()
    }

    override suspend fun handleIntent(intent: BookmarkedShopListIntent) {
        when (intent) {
            BookmarkedShopListIntent.OnRetry -> retryCurrentBookmarks()
            is BookmarkedShopListIntent.OnRemovalConfirmed -> handleRemovalConfirmed(intent)
        }
    }

    private fun retryCurrentBookmarks() {
        when (val state = personalizationStore.state.value) {
            is PersonalizationBootstrapState.Success -> syncBookmarkedShops(state.value.bookmarkedShopIds, forceFetch = true)
            PersonalizationBootstrapState.Error -> refreshPersonalization()
            PersonalizationBootstrapState.Loading -> Unit
        }
    }

    private fun handleRemovalConfirmed(intent: BookmarkedShopListIntent.OnRemovalConfirmed) {
        removeBookmark(intent.shopId)
    }

    private fun observeBookmarkedShopIds() {
        viewModelScope.launch {
            personalizationStore.state.collectLatest { state ->
                when (state) {
                    is PersonalizationBootstrapState.Success -> syncBookmarkedShops(state.value.bookmarkedShopIds)
                    PersonalizationBootstrapState.Error -> clearBookmarkedShops(showError = true)
                    PersonalizationBootstrapState.Loading -> clearBookmarkedShops(showError = false)
                }
            }
        }
    }

    private fun clearBookmarkedShops(showError: Boolean) {
        cancelTask(FETCH_BOOKMARKS_TASK_KEY)
        reduce { copy(shops = RamenShops(emptyMap()), showError = showError, hasLoaded = showError) }
    }

    private fun refreshPersonalization() {
        launchResultTask(
            taskKey = PERSONALIZATION_TASK_KEY,
            loadKey = BookmarkedShopLoadKey.PERSONALIZATION,
            policy = TaskPolicy.IgnoreNew,
            request = personalizationStore::refresh,
        )
    }

    private fun syncBookmarkedShops(
        shopIds: Set<String>,
        forceFetch: Boolean = false,
    ) {
        val isShopIdsEmpty = shopIds.isEmpty()
        if (isShopIdsEmpty) {
            applyExistingBookmarkedShops(shopIds)
            return
        }

        val isAlreadyCached = !forceFetch && currentState.shops.containsAll(shopIds)
        if (isAlreadyCached) {
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
                shops = shops.filterByShopIds(requestedShopIds),
                showError = false,
                hasLoaded = true,
            )
        }
    }

    private fun removeBookmark(shopId: String) {
        currentState.shops[shopId]?.let { shop ->
            analyticsTracker.logEvent(
                BookmarkToggled(
                    shopId = shop.id,
                    shopName = shop.name,
                    enabled = false,
                    source = AnalyticsSource.BOOKMARKED_SHOPS,
                ),
            )
        }
        launchResultTask(
            taskKey = REMOVE_BOOKMARK_TASK_KEY,
            loadKey = BookmarkedShopLoadKey.REMOVE,
            policy = TaskPolicy.IgnoreNew,
            request = { personalizationStore.updateBookmark(shopId, false) },
            onSuccess = { handleBookmarkRemovalSuccess(shopId) },
            onError = { showToast(Res.string.personalization_update_failure_message, type = ToastType.ERROR) },
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
        private const val PERSONALIZATION_TASK_KEY = "refresh-bookmark-personalization"
        private const val FETCH_BOOKMARKS_TASK_KEY = "fetch-bookmarks"
        private const val REMOVE_BOOKMARK_TASK_KEY = "remove-bookmark"
    }
}
