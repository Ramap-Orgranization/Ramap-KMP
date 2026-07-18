package com.peto.ramap.ui.bookmark

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListIntent
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListSideEffect
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListUiState
import com.peto.ramap.ui.common.LoadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmark_removal_success_message
import ramap.shared.generated.resources.personalization_update_failure_message

class BookmarkedShopListViewModel(
    private val personalizationRepository: ShopPersonalizationStore,
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<BookmarkedShopListUiState, BookmarkedShopListIntent, BookmarkedShopListSideEffect>(
        BookmarkedShopListUiState(),
    ) {
    private var bookmarkObservationJob: Job? = null

    init {
        restartBookmarkObservation()
    }

    override suspend fun handleIntent(intent: BookmarkedShopListIntent) {
        when (intent) {
            BookmarkedShopListIntent.Retry -> restartBookmarkObservation()
            is BookmarkedShopListIntent.OnRemovalConfirmed -> removeBookmark(intent.shopId)
        }
    }

    private fun restartBookmarkObservation() {
        bookmarkObservationJob?.cancel()
        bookmarkObservationJob = viewModelScope.launch { loadAndObserveBookmarks() }
    }

    private suspend fun loadAndObserveBookmarks() {
        reduce { copy(shopsState = LoadState.Loading) }
        personalizationRepository.state.collectLatest { personalization ->
            updateBookmarkedShops(personalization.bookmarkedShopIds)
        }
    }

    private suspend fun updateBookmarkedShops(shopIds: Set<String>) {
        val retainedShops = currentRetainedShops(shopIds)
        val addedShopIds = shopIds - retainedShops.keys

        if (addedShopIds.isEmpty()) {
            updateShopContent(retainedShops)
            return
        }

        loadRamenShops(retainedShops, addedShopIds, shopIds)
    }

    private fun currentRetainedShops(shopIds: Set<String>): RamenShops {
        val currentShops =
            (currentState.shopsState as? LoadState.Content)?.data ?: RamenShops(emptyMap())
        return currentShops.filterByShopIds(shopIds)
    }

    private suspend fun loadRamenShops(
        retainedShops: RamenShops,
        addedShopIds: Set<String>,
        shopIds: Set<String>,
    ) {
        handleResult(
            result = ramenShopRepository.fetchRamenShops(addedShopIds),
            onSuccess = { fetchedShops ->
                updateShopContent(
                    RamenShops(retainedShops + fetchedShops.filterByShopIds(shopIds)),
                )
            },
            onError = { reduce { copy(shopsState = LoadState.Error) } },
        )
    }

    private fun updateShopContent(shops: RamenShops) {
        reduce {
            copy(shopsState = LoadState.Content(shops.sortedByName()))
        }
    }

    private suspend fun removeBookmark(shopId: String) {
        handleResult(
            result = personalizationRepository.updateBookmark(shopId, false),
            onSuccess = {
                showToast(
                    Res.string.bookmark_removal_success_message,
                    ToastType.SUCCESS,
                )
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
}
