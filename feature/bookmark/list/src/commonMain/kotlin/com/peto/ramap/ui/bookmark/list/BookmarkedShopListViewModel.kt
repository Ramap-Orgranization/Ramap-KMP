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
    private var bookmarkedShopIds: Set<String>? = null
    private var resolvedShopIds = emptySet<String>()

    init {
        observeBookmarkedShopIds()
    }

    override suspend fun handleIntent(intent: BookmarkedShopListIntent) {
        when (intent) {
            BookmarkedShopListIntent.OnRetry -> retryCurrentBookmarks()
            BookmarkedShopListIntent.LoadMore -> if (!currentState.appendFailed) loadMore()
            is BookmarkedShopListIntent.OnRemovalConfirmed -> handleRemovalConfirmed(intent)
        }
    }

    private fun retryCurrentBookmarks() {
        when (personalizationStore.state.value) {
            is PersonalizationBootstrapState.Success -> loadMore()
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
        bookmarkedShopIds = null
        resolvedShopIds = emptySet()
        reduce {
            copy(
                shops = RamenShops(emptyMap()),
                remainingShopCount = 0,
                showError = showError,
                appendFailed = false,
                hasLoaded = showError,
            )
        }
    }

    private fun refreshPersonalization() {
        launchResultTask(
            taskKey = PERSONALIZATION_TASK_KEY,
            loadKey = BookmarkedShopLoadKey.PERSONALIZATION,
            policy = TaskPolicy.IgnoreNew,
            request = personalizationStore::refresh,
        )
    }

    private fun syncBookmarkedShops(shopIds: Set<String>) {
        if (bookmarkedShopIds == shopIds) return

        bookmarkedShopIds = shopIds
        resolvedShopIds = resolvedShopIds intersect shopIds

        val remaining = remainingShopIds()
        val visibleShops = currentState.shops.filterByShopIds(shopIds)
        val shouldFetch = remaining.isNotEmpty() && visibleShops.isEmpty()
        if (remaining.isEmpty() || shouldFetch) cancelTask(FETCH_BOOKMARKS_TASK_KEY)

        reduce {
            copy(
                shops = visibleShops,
                remainingShopCount = remaining.size,
                showError = false,
                appendFailed = appendFailed && remaining.isNotEmpty() && !shouldFetch,
                hasLoaded = remaining.isEmpty() || visibleShops.isNotEmpty(),
            )
        }
        if (shouldFetch) fetchNextPage()
    }

    private fun loadMore() {
        val latest = personalizationStore.state.value as? PersonalizationBootstrapState.Success ?: return
        if (latest.value.bookmarkedShopIds != bookmarkedShopIds || currentState.isPageLoading) return
        fetchNextPage()
    }

    private fun remainingShopIds(): Set<String> = bookmarkedShopIds.orEmpty() - resolvedShopIds

    private fun fetchNextPage() {
        val pageIds = remainingShopIds().take(PAGE_SIZE).toSet()
        if (pageIds.isEmpty()) return
        launchResultTask(
            taskKey = FETCH_BOOKMARKS_TASK_KEY,
            loadKey = BookmarkedShopLoadKey.FETCH,
            onStart = { copy(showError = false, appendFailed = false) },
            request = { ramenShopRepository.fetchRamenShops(pageIds) },
            onSuccess = { shops -> applyPage(shops, pageIds) },
            onError = { reduce { copy(showError = shops.isEmpty(), appendFailed = shops.isNotEmpty(), hasLoaded = true) } },
        )
    }

    private fun applyPage(
        fetchedShops: RamenShops,
        requestedShopIds: Set<String>,
    ) {
        val activeIds = requestedShopIds intersect bookmarkedShopIds.orEmpty()
        resolvedShopIds = resolvedShopIds + activeIds
        val remaining = remainingShopIds()
        val page = activeIds.mapNotNull(fetchedShops::get)
        reduce {
            val combinedShops = RamenShops(shops.values + page)
            copy(
                shops = combinedShops,
                remainingShopCount = remaining.size,
                hasLoaded = combinedShops.isNotEmpty() || remaining.isEmpty(),
            )
        }
        if (!currentState.hasLoaded) fetchNextPage()
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
            onSuccess = { handleBookmarkRemovalSuccess() },
            onError = { showToast(Res.string.personalization_update_failure_message, type = ToastType.ERROR) },
        )
    }

    private fun handleBookmarkRemovalSuccess() {
        val state = personalizationStore.state.value as? PersonalizationBootstrapState.Success
        state?.let { syncBookmarkedShops(it.value.bookmarkedShopIds) }
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
        private const val PAGE_SIZE = 20
        private const val PERSONALIZATION_TASK_KEY = "refresh-bookmark-personalization"
        private const val FETCH_BOOKMARKS_TASK_KEY = "fetch-bookmarks"
        private const val REMOVE_BOOKMARK_TASK_KEY = "remove-bookmark"
    }
}
