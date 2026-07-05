package com.peto.ramap.ui.map

import com.peto.ramap.core.base.BaseViewModel
import com.peto.ramap.core.config.MarkerClusterConfig
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShopFilter
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.SearchQuery
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.ui.map.contract.MapIntent
import com.peto.ramap.ui.map.contract.MapSideEffect
import com.peto.ramap.ui.map.contract.MapUiState
import com.peto.ramap.ui.map.model.MapPersonalization
import com.peto.ramap.ui.map.model.SearchUiState
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.account_delete_unavailable_message
import ramap.shared.generated.resources.filter_empty_visible_result_message
import ramap.shared.generated.resources.hidden_shop_search_result_message
import kotlin.time.Duration.Companion.milliseconds

class MapViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val shopWaitingSystemRepository: ShopWaitingSystemRepository,
    private val personalizationRepository: PersonalizationRepository,
    private val loginRepository: LoginRepository,
) : BaseViewModel<MapUiState, MapIntent, MapSideEffect>(initialState = MapUiState()) {
    private var boundsLoadJob: Job? = null
    private var boundsLoadRequestId = 0L
    private var lastLoadedBounds: MapBounds? = null
    private var searchJob: Job? = null
    private var searchRequestId = 0L

    init {
        runTask { observeSessionStatus() }
    }

    override suspend fun handleIntent(intent: MapIntent) {
        when (intent) {
            is MapIntent.OnBoundsChanged -> scheduleRamenShopsLoad(intent.bounds)
            is MapIntent.OnMyLocationChanged -> updateMyLocation(intent.location)
            is MapIntent.OnShopSelected -> selectShop(intent.shop)
            is MapIntent.OnShopDetailDismissed -> dismissShopDetail()
            is MapIntent.OnSearchResultsDismissed -> dismissSearchResults()
            is MapIntent.OnQueryChanged -> updateQuery(intent.query)
            is MapIntent.OnCategoryFilterToggled -> toggleCategoryFilter(intent.category)
            is MapIntent.OnFilterCleared -> clearFilter()
            is MapIntent.OnBookmarkToggled -> toggleBookmark(intent.shop)
            is MapIntent.OnHiddenToggled -> toggleHidden(intent.shop)
            is MapIntent.OnPersonalizationViewChanged -> changePersonalizationView(intent.view)
            MapIntent.OnKakaoLoginClicked -> signInWithKakao()
            MapIntent.OnLogoutClicked -> signOut()
            MapIntent.OnAccountDeleteClicked -> showToast(Res.string.account_delete_unavailable_message)
        }
    }

    private suspend fun observeSessionStatus() {
        loginRepository.sessionStatus.collectLatest { status ->
            val isAuthenticated = status is SessionStatus.Authenticated
            updateAuthState(isAuthenticated)

            if (isAuthenticated) loadPersonalization()
        }
    }

    private fun updateAuthState(isAuthenticated: Boolean) {
        reduce {
            copy(
                isLoggedIn = isAuthenticated,
                accountLabel = if (isAuthenticated) loginRepository.currentUserEmail() else null,
                bookmarkedShopIds = if (isAuthenticated) bookmarkedShopIds else emptySet(),
                hiddenShopIds = if (isAuthenticated) hiddenShopIds else emptySet(),
                personalizationView =
                    if (isAuthenticated) {
                        personalizationView
                    } else {
                        MapPersonalization.ALL
                    },
            )
        }
    }

    private fun selectShop(shop: RamenShop) {
        val isCurrentSearchResult = shop.id in currentState.search.results
        reduce {
            copy(
                selectedShop = shop,
                search = search.consumeResultFocus(isCurrentSearchResult),
            )
        }
        runTask { loadShopWaitingSystem(shop.id) }
    }

    private fun dismissShopDetail() {
        reduce { copy(selectedShop = null) }
    }

    private fun updateMyLocation(location: Location) {
        reduce { copy(currentLocation = location) }
    }

    private fun dismissSearchResults() {
        reduce { copy(search = search.dismissResults()) }
    }

    private fun updateQuery(query: String) {
        val normalizedQuery = SearchQuery(query).normalizeShopSearchQuery()
        val hasCurrentSearchResults = currentState.search.hasLoadedResultsFor(normalizedQuery)

        reduce {
            copy(
                search = search.updateInput(query),
                selectedShop = null,
            )
        }

        if (normalizedQuery.value.isNotBlank() && hasCurrentSearchResults) {
            searchJob?.cancel()
            runTask { handleSingleSearchResult(currentState.searchResultShops.singleOrNull()) }
            return
        }

        scheduleSearch(normalizedQuery)
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

    private suspend fun loadPersonalization() {
        val result = personalizationRepository.fetchPersonalization()

        val hiddenShopIds = result.hiddenShopIds
        val bookmarkedShopIds = result.bookmarkedShopIds - hiddenShopIds

        loadPersonalizedShops(
            bookmarkedShopIds + hiddenShopIds,
        )

        reduce {
            copy(
                bookmarkedShopIds = bookmarkedShopIds,
                hiddenShopIds = hiddenShopIds,
                selectedShop = selectedShop?.takeIf { it.id !in hiddenShopIds },
            )
        }
    }

    private suspend fun loadPersonalizedShops(shopIds: Set<String>) {
        if (shopIds.isEmpty()) return

        val result = ramenShopRepository.fetchRamenShopsByIds(shopIds)
        reduce { copy(shops = RamenShops(shops + result)) }
    }

    private fun toggleBookmark(shop: RamenShop) {
        if (!currentState.isLoggedIn) {
            runTask { postSideEffect(MapSideEffect.ShowLoginGuide) }
            return
        }

        runTask {
            val isBookmarked = shop.id in currentState.bookmarkedShopIds

            updateBookmarkPersonalization(
                shopId = shop.id,
                isBookmarked = isBookmarked,
            )
            reduceBookmarkState(
                shopId = shop.id,
                isBookmarked = isBookmarked,
            )
        }
    }

    private suspend fun updateBookmarkPersonalization(
        shopId: String,
        isBookmarked: Boolean,
    ) {
        if (isBookmarked) {
            personalizationRepository.removeBookmark(shopId)
            return
        }

        personalizationRepository.addBookmark(shopId)
        loadPersonalizedShops(setOf(shopId))
    }

    private fun reduceBookmarkState(
        shopId: String,
        isBookmarked: Boolean,
    ) {
        reduce {
            copy(
                bookmarkedShopIds =
                    if (isBookmarked) {
                        bookmarkedShopIds - shopId
                    } else {
                        bookmarkedShopIds + shopId
                    },
            )
        }
    }

    private fun toggleHidden(shop: RamenShop) {
        if (!currentState.isLoggedIn) {
            runTask { postSideEffect(MapSideEffect.ShowLoginGuide) }
            return
        }

        runTask {
            val isHidden = shop.id in currentState.hiddenShopIds
            val shouldRemoveBookmark = shop.id in currentState.bookmarkedShopIds

            updateHiddenShopPersonalization(
                shopId = shop.id,
                isHidden = isHidden,
                shouldRemoveBookmark = shouldRemoveBookmark,
            )
            reduceHiddenShopState(
                shopId = shop.id,
                isHidden = isHidden,
                shouldRemoveBookmark = shouldRemoveBookmark,
            )
        }
    }

    private suspend fun updateHiddenShopPersonalization(
        shopId: String,
        isHidden: Boolean,
        shouldRemoveBookmark: Boolean,
    ) {
        if (isHidden) {
            personalizationRepository.unhideShop(shopId)
            return
        }

        personalizationRepository.hideShop(shopId)
        if (shouldRemoveBookmark) {
            personalizationRepository.removeBookmark(shopId)
        }
    }

    private fun reduceHiddenShopState(
        shopId: String,
        isHidden: Boolean,
        shouldRemoveBookmark: Boolean,
    ) {
        reduce {
            val shouldCloseSelectedShop = shouldCloseSelectedShop(shopId, isHidden)

            copy(
                hiddenShopIds =
                    if (isHidden) {
                        hiddenShopIds - shopId
                    } else {
                        hiddenShopIds + shopId
                    },
                bookmarkedShopIds =
                    if (!isHidden && shouldRemoveBookmark) {
                        bookmarkedShopIds - shopId
                    } else {
                        bookmarkedShopIds
                    },
                selectedShop =
                    updateSelectedShop(
                        selectedShop = selectedShop,
                        shopId = shopId,
                        isHidden = isHidden,
                        shouldCloseSelectedShop = shouldCloseSelectedShop,
                    ),
                search =
                    updateSearchState(
                        search = search,
                        shouldCloseSelectedShop = shouldCloseSelectedShop,
                    ),
            )
        }
    }

    private fun shouldCloseSelectedShop(
        shopId: String,
        isHidden: Boolean,
    ): Boolean = !isHidden && currentState.selectedShop?.id == shopId

    private fun updateSelectedShop(
        selectedShop: RamenShop?,
        shopId: String,
        isHidden: Boolean,
        shouldCloseSelectedShop: Boolean,
    ): RamenShop? {
        if (selectedShop == null || shouldCloseSelectedShop) return null

        return if (isHidden && selectedShop.id == shopId) {
            selectedShop.copy(isVisible = true)
        } else {
            selectedShop
        }
    }

    private fun updateSearchState(
        search: SearchUiState,
        shouldCloseSelectedShop: Boolean,
    ): SearchUiState =
        if (shouldCloseSelectedShop) {
            search.dismissResults()
        } else {
            search
        }

    private fun changePersonalizationView(view: MapPersonalization) {
        if (!currentState.isLoggedIn && view != MapPersonalization.ALL) {
            runTask { postSideEffect(MapSideEffect.ShowLoginGuide) }
            return
        }

        reduce {
            copy(
                personalizationView = view,
                selectedShop =
                    selectedShop?.takeIf { shop ->
                        when (view) {
                            MapPersonalization.ALL -> shop.id !in hiddenShopIds
                            MapPersonalization.BOOKMARKED -> shop.id in bookmarkedShopIds
                            MapPersonalization.HIDDEN -> shop.id in hiddenShopIds
                        }
                    },
                search = search.showResults(),
            )
        }
    }

    private fun signInWithKakao() {
        runTask {
            loginRepository.signInWithKakao()
        }
    }

    private fun signOut() {
        runTask {
            loginRepository.signOut()
        }
    }

    private fun updateFilter(filter: RamenShopFilter) {
        reduce {
            copy(
                filters = filter,
                selectedShop =
                    selectedShop?.takeIf { shop ->
                        shop.menuCategories.matches(filter)
                    },
                search = search.showResults(),
            )
        }
        showEmptyFilterResultMessageIfNeeded()
    }

    private fun showEmptyFilterResultMessageIfNeeded() {
        val state = currentState
        if (state.filters.isEmpty() || state.markerShops.hasVisibleShopIn(state.bounds)) return

        showToast(Res.string.filter_empty_visible_result_message)
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
                search = search.clearResults(),
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

        reduceSearchResult(query, result)

        handleSingleSearchResult(currentState.searchResultShops.singleOrNull())
    }

    private fun handleSingleSearchResult(shop: RamenShop?) {
        when {
            shop == null -> Unit
            shop.isVisible -> selectShop(shop)
            else -> showToast(Res.string.hidden_shop_search_result_message)
        }
    }

    private fun showToast(messageResource: StringResource) {
        runTask { postSideEffect(MapSideEffect.ShowToast(messageResource)) }
    }

    private fun reduceSearchResult(
        query: SearchQuery,
        result: RamenShops,
    ) {
        reduce {
            copy(
                search = search.updateResults(query, result),
                selectedShop = null,
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
                    if (
                        bounds.hasMeaningfulZoomChangeFrom(
                            other = clusterBounds,
                            zoomShiftRatio = MarkerClusterConfig.ZOOM_SHIFT_RATIO,
                        )
                    ) {
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
        if (previousBounds != null && !bounds.hasMeaningfulViewportChangeFrom(previousBounds)) return

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

    companion object {
        private const val BOUNDS_LOAD_DEBOUNCE_MILLIS = 350L
        private const val SEARCH_DEBOUNCE_MILLIS = 300L
        private const val SEARCH_RESULT_LIMIT = 50
    }
}
