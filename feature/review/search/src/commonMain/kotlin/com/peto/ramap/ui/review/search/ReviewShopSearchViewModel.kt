package com.peto.ramap.ui.review.search

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.platform.storage.SearchHistoryStorage
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.review.search.contract.ReviewShopSearchIntent
import com.peto.ramap.ui.review.search.contract.ReviewShopSearchLoadKey
import com.peto.ramap.ui.review.search.contract.ReviewShopSearchSideEffect
import com.peto.ramap.ui.review.search.contract.ReviewShopSearchUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class ReviewShopSearchViewModel(
    private val searchShops: SearchRamenShopsUseCase,
    private val repository: RamenShopRepository,
    private val history: SearchHistoryStorage,
) : BaseViewModel<ReviewShopSearchUiState, ReviewShopSearchIntent, ReviewShopSearchSideEffect>(ReviewShopSearchUiState()) {
    init {
        loadRecentShops()
    }

    override suspend fun handleIntent(intent: ReviewShopSearchIntent) {
        when (intent) {
            is ReviewShopSearchIntent.QueryChanged -> changeQuery(intent.query)
            ReviewShopSearchIntent.Search -> search()
            ReviewShopSearchIntent.Retry -> if (currentState.isShowingHistory) loadRecentShops() else search()
            is ReviewShopSearchIntent.SelectShop -> selectShop(intent.shopId)
        }
    }

    private fun changeQuery(input: String) {
        cancelTask(SEARCH_TASK)
        reduce {
            copy(query = input.take(SearchQuery.MAX_LENGTH), results = RamenShops(emptyMap()), hasSearched = false, hasSearchError = false)
        }
        search(debounce = true)
    }

    private fun search(debounce: Boolean = false) {
        val query = SearchQuery(currentState.query).normalizeShopSearchQuery()
        if (!query.isValidForShopSearch()) return
        launchResultTask(
            taskKey = SEARCH_TASK,
            loadKey = ReviewShopSearchLoadKey.Search,
            onStart = { copy(hasSearchError = false, hasSearched = false) },
            request = {
                if (debounce) delay(SEARCH_DEBOUNCE_MS)
                searchShops(query)
            },
            onSuccess = { shops -> reduce { copy(results = shops, hasSearched = true) } },
            onError = { reduce { copy(hasSearchError = true) } },
        )
    }

    private fun loadRecentShops() {
        launchResultTask(
            taskKey = RECENT_SHOPS_TASK,
            loadKey = ReviewShopSearchLoadKey.RecentShops,
            onStart = { copy(hasRecentShopsError = false) },
            request = {
                val ids = history.recentlyViewedShopIds.first()
                when (val result = if (ids.isEmpty()) RamapResult.Success(RamenShops(emptyMap())) else repository.fetchRamenShops(ids.toSet())) {
                    is RamapResult.Success -> RamapResult.Success(RamenShops(ids.mapNotNull { result.data[it] }))
                    is RamapResult.Error -> result
                }
            },
            onSuccess = { shops -> reduce { copy(recentShops = shops) } },
            onError = { reduce { copy(hasRecentShopsError = true) } },
        )
    }

    private suspend fun selectShop(shopId: String) {
        if (shopId !in currentState.shops) return
        postSideEffect(ReviewShopSearchSideEffect.WriteReview(shopId))
        launchTask(taskKey = RECORD_HISTORY_TASK) {
            history.addRecentlyViewedShop(shopId)
        }
    }

    companion object {
        private const val SEARCH_TASK = "review-shop-search"
        private const val RECENT_SHOPS_TASK = "review-recent-shops"
        private const val RECORD_HISTORY_TASK = "review-shop-history"
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
