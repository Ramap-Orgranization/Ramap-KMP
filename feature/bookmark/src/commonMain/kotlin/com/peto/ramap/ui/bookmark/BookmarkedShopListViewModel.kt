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
        BookmarkedShopListUiState(),
    ) {
    init {
        viewModelScope.launch {
            personalizationStore.state
                .map { it.bookmarkedShopIds }
                .distinctUntilChanged()
                .collectLatest(::fetchBookmarkedShops)
        }
    }

    override suspend fun handleIntent(intent: BookmarkedShopListIntent) {
        when (intent) {
            is BookmarkedShopListIntent.OnRemovalConfirmed -> removeBookmark(intent.shopId)
        }
    }

    private suspend fun fetchBookmarkedShops(shopIds: Set<String>) {
        reduce { copy(shopsState = LoadState.Loading) }
        if (shopIds.isEmpty()) {
            reduce { copy(shopsState = LoadState.Content(RamenShops(emptyMap()))) }
            return
        }
        handleResult(
            result = ramenShopRepository.fetchRamenShops(shopIds),
            onSuccess = { shops ->
                reduce {
                    copy(shopsState = LoadState.Content(shops.filterByShopIds(shopIds)))
                }
            },
            onError = { reduce { copy(shopsState = LoadState.Error) } },
        )
    }

    private suspend fun removeBookmark(shopId: String) {
        handleResult(
            result = personalizationStore.updateBookmark(shopId, false),
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
