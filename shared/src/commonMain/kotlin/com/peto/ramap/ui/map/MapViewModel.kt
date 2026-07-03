package com.peto.ramap.ui.map

import com.peto.ramap.core.base.BaseViewModel
import com.peto.ramap.core.config.MarkerClusterConfig
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShopFilter
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.SearchQuery
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.ui.map.contract.MapIntent
import com.peto.ramap.ui.map.contract.MapSideEffect
import com.peto.ramap.ui.map.contract.MapUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

class MapViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val shopWaitingSystemRepository: ShopWaitingSystemRepository,
) : BaseViewModel<MapUiState, MapIntent, MapSideEffect>(initialState = MapUiState()) {
    private var boundsLoadJob: Job? = null
    private var boundsLoadRequestId = 0L
    private var lastLoadedBounds: MapBounds? = null
    private var searchJob: Job? = null
    private var searchRequestId = 0L

    override suspend fun handleIntent(intent: MapIntent) {
        when (intent) {
            is MapIntent.OnBoundsChanged -> scheduleRamenShopsLoad(intent.bounds)
            is MapIntent.OnShopSelected -> selectShop(intent.shop)
            is MapIntent.OnShopDetailDismissed -> dismissShopDetail()
            is MapIntent.OnSearchResultsDismissed -> dismissSearchResults()
            is MapIntent.OnQueryChanged -> updateQuery(intent.query)
            is MapIntent.OnCategoryFilterToggled -> toggleCategoryFilter(intent.category)
            is MapIntent.OnFilterCleared -> clearFilter()
        }
    }

    private fun selectShop(shop: RamenShop) {
        reduce { copy(selectedShop = shop) }
        runTask { loadShopWaitingSystem(shop.id) }
    }

    private fun dismissShopDetail() {
        reduce { copy(selectedShop = null) }
    }

    private fun dismissSearchResults() {
        reduce { copy(isSearchResultsDismissed = true) }
    }

    private fun updateQuery(query: String) {
        reduce {
            copy(
                query = query,
                isSearchResultsDismissed = false,
            )
        }
        scheduleSearch(SearchQuery(query).normalizeShopSearchQuery())
    }

    private fun toggleCategoryFilter(category: Category) {
        val currentFilter = currentState.filters
        val nextFilter =
            if (category in currentFilter) {
                currentFilter - category
            } else {
                currentFilter + category
            }

        updateFilter(nextFilter)
    }

    private fun clearFilter() {
        updateFilter(RamenShopFilter())
    }

    private fun updateFilter(filter: RamenShopFilter) {
        reduce {
            copy(
                filters = filter,
                selectedShop =
                    selectedShop?.takeIf { shop ->
                        shop.menuCategories.matches(filter)
                    },
                isSearchResultsDismissed = false,
            )
        }
        showEmptyFilterResultMessageIfNeeded()
    }

    private fun showEmptyFilterResultMessageIfNeeded() {
        val state = currentState
        if (state.filters.isEmpty() || state.markerShops.hasVisibleShopIn(state.bounds)) return

        runTask {
            postSideEffect(MapSideEffect.ShowToast)
        }
    }

    private fun scheduleSearch(query: SearchQuery) {
        searchJob?.cancel()
        val requestId = ++searchRequestId

        if (query.value.isBlank()) {
            clearSearchResults()
            return
        }

        loadSearch(query, requestId)
    }

    private fun loadSearch(
        query: SearchQuery,
        requestId: Long,
    ) {
        searchJob =
            runTask {
                delay(SEARCH_DEBOUNCE_MILLIS.milliseconds)
                loadSearchResults(query, requestId)
            }
    }

    private fun clearSearchResults() {
        reduce {
            copy(
                searchResults = RamenShops(emptyMap()),
                searchResultsQuery = null,
                isSearchResultsDismissed = false,
            )
        }
    }

    private suspend fun loadSearchResults(
        query: SearchQuery,
        requestId: Long,
    ) {
        val result: RamenShops =
            ramenShopRepository.searchRamenShops(
                query = query,
                limit = SEARCH_RESULT_LIMIT,
            )
        if (requestId != searchRequestId) return

        reduceSearchResult(
            query = query,
            result = result,
        )

        result.filterByCategory(currentState.filters).values.singleOrNull()?.let { shop ->
            selectShop(shop)
        }
    }

    private fun reduceSearchResult(
        query: SearchQuery,
        result: RamenShops,
    ) {
        reduce {
            copy(
                searchResults = result,
                searchResultsQuery = query,
            )
        }
    }

    private suspend fun loadShopWaitingSystem(shopId: String) {
        if (currentState.shopWaiting.containsKey(shopId)) return

        val waitingSystem = shopWaitingSystemRepository.fetchShopWaitingSystem(shopId)

        reduce {
            copy(shopWaiting = shopWaiting + (shopId to waitingSystem))
        }
    }

    /**
     * 지도 이동 이벤트를 바로 API 호출로 연결하지 않고 짧게 지연한다.
     *
     * 사용자가 지도를 연속해서 움직이면 이전 작업을 취소하고 마지막 bounds만 남겨,
     * 드래그 중간 지점마다 라멘 가게 목록을 다시 조회하지 않도록 한다.
     */
    private fun scheduleRamenShopsLoad(bounds: MapBounds) {
        reduce {
            copy(
                bounds = bounds,
                clusterBounds =
                    if (bounds.isZoomMeaningfullyDifferentFrom(clusterBounds)) {
                        bounds
                    } else {
                        clusterBounds
                    },
            )
        }

        boundsLoadJob?.cancel()
        val requestId = ++boundsLoadRequestId
        boundsLoadJob =
            runTask {
                delay(BOUNDS_LOAD_DEBOUNCE_MILLIS.milliseconds)
                loadRamenShops(bounds, requestId)
            }
    }

    /**
     * 마지막 성공 조회 영역과 비교해 충분히 달라진 경우에만 목록을 조회한다.
     *
     * 요청 취소를 협조하지 못한 오래된 작업이 늦게 끝나더라도 최신 request id와 다르면
     * 결과를 버리고, 조회 결과가 기존 UI 상태와 같으면 state 갱신도 생략해 마커 재렌더링을 줄인다.
     */
    private suspend fun loadRamenShops(
        bounds: MapBounds,
        requestId: Long,
    ) {
        val previousBounds = lastLoadedBounds
        if (previousBounds != null && !bounds.isMeaningfullyDifferentFrom(previousBounds)) return

        val result: RamenShops = ramenShopRepository.fetchRamenShops(bounds)
        if (requestId != boundsLoadRequestId) return

        lastLoadedBounds = bounds

        reduceLoadRamenShopResult(result)
    }

    private fun reduceLoadRamenShopResult(result: RamenShops) {
        val mergedShops = mergeShops(result)
        if (currentState.shops != mergedShops) {
            reduce { copy(shops = mergedShops) }
        }
    }

    private fun mergeShops(newShops: RamenShops): RamenShops =
        RamenShops(
            currentState.shops + newShops,
        )

    private fun MapBounds.isZoomMeaningfullyDifferentFrom(other: MapBounds): Boolean =
        latSpan.isMeaningfullyDifferentFrom(other.latSpan, MarkerClusterConfig.ZOOM_SHIFT_RATIO) ||
            lngSpan.isMeaningfullyDifferentFrom(other.lngSpan, MarkerClusterConfig.ZOOM_SHIFT_RATIO)

    private fun Double.isMeaningfullyDifferentFrom(
        other: Double,
        ratio: Double,
    ): Boolean {
        if (other == 0.0) return this != 0.0
        return abs(this - other) / abs(other) >= ratio
    }

    companion object {
        private const val BOUNDS_LOAD_DEBOUNCE_MILLIS = 350L
        private const val SEARCH_DEBOUNCE_MILLIS = 300L
        private const val SEARCH_RESULT_LIMIT = 50
    }
}
