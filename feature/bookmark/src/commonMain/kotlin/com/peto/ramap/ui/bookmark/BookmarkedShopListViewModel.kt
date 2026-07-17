package com.peto.ramap.ui.bookmark

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListIntent
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListSideEffect
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListUiState
import com.peto.ramap.ui.common.LoadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.personalization_update_failure_message

class BookmarkedShopListViewModel(
    private val personalizationRepository: PersonalizationRepository,
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<BookmarkedShopListUiState, BookmarkedShopListIntent, BookmarkedShopListSideEffect>(
        BookmarkedShopListUiState(),
    ) {
    private var bookmarkObservationJob: Job? = null

    init {
        restartLoading()
    }

    override suspend fun handleIntent(intent: BookmarkedShopListIntent) {
        when (intent) {
            BookmarkedShopListIntent.Retry -> restartLoading()
            is BookmarkedShopListIntent.OnShopClicked ->
                reduce { copy(pendingBookmarkShopId = intent.shopId) }
            BookmarkedShopListIntent.OnRemovalConfirmed -> removePendingBookmark()
            BookmarkedShopListIntent.OnRemovalDismissed ->
                reduce { copy(pendingBookmarkShopId = null) }
        }
    }

    private suspend fun removePendingBookmark() {
        val shopId = currentState.pendingBookmarkShopId ?: return
        when (personalizationRepository.removeBookmark(shopId)) {
            is RamapResult.Success ->
                reduce { copy(pendingBookmarkShopId = null) }
            is RamapResult.Error -> {
                reduce { copy(pendingBookmarkShopId = null) }
                trySideEffect(
                    BookmarkedShopListSideEffect.ShowToast(
                        ToastData(Res.string.personalization_update_failure_message, ToastType.ERROR),
                    ),
                )
            }
        }
    }

    private fun restartLoading() {
        bookmarkObservationJob?.cancel()
        bookmarkObservationJob = viewModelScope.launch { loadAndObserveBookmarks() }
    }

    private suspend fun loadAndObserveBookmarks() {
        reduce { copy(shopsState = LoadState.Loading) }
        handleResult(
            result = personalizationRepository.fetchPersonalization(),
            onSuccess = {
                personalizationRepository.bookmarkedShopIds.collectLatest { shopIds ->
                    updateBookmarkedShops(shopIds)
                }
            },
            onError = { reduce { copy(shopsState = LoadState.Error) } },
        )
    }

    private suspend fun updateBookmarkedShops(shopIds: Set<String>) {
        val currentShops = (currentState.shopsState as? LoadState.Content)?.data ?: RamenShops(emptyMap())
        val retainedShops = currentShops.filterByShopIds(shopIds)
        val addedShopIds = shopIds - retainedShops.keys

        if (addedShopIds.isEmpty()) {
            updateShopContent(retainedShops, shopIds)
            return
        }

        when (val result = ramenShopRepository.fetchRamenShopsByIds(addedShopIds)) {
            is RamapResult.Success ->
                updateShopContent(
                    shops = RamenShops(retainedShops + result.data.filterByShopIds(shopIds)),
                    shopIds = shopIds,
                )
            is RamapResult.Error -> reduce { copy(shopsState = LoadState.Error) }
        }
    }

    private fun updateShopContent(
        shops: RamenShops,
        shopIds: Set<String>,
    ) {
        reduce {
            copy(
                shopsState = LoadState.Content(shops.sortedByName()),
                pendingBookmarkShopId = pendingBookmarkShopId?.takeIf { it in shopIds },
            )
        }
    }
}
