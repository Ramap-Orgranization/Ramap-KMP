package com.peto.ramap.ui.main.map

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.place.PlaceSearchErrorToast
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShopFilter
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.platform.storage.SearchHistoryStorage
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.location.CurrentLocationStore
import com.peto.ramap.ui.main.map.contract.MapIntent
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkedShopsToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBoundsChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCameraPositionChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCategoryFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnInitialLocationFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLocationPermissionBlocked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMapTabExited
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMyLocationChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnOpenFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnOperatingNoticesRefreshRequested
import com.peto.ramap.ui.main.map.contract.MapIntent.OnQueryChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchDeleted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchesCleared
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRequestedShopDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSearchResultsDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSelectedShopFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopIdSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopNavigationRequested
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnStatusTimeRefreshed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnViewportLoadRetry
import com.peto.ramap.ui.main.map.contract.MapLoadKey
import com.peto.ramap.ui.main.map.contract.MapSideEffect
import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.main.map.log.MapAnalytics
import com.peto.ramap.ui.main.map.model.CameraPosition
import com.peto.ramap.ui.main.map.model.ShopFocusRequest
import com.peto.ramap.ui.main.map.model.location.LocationFocusStatus
import com.peto.ramap.ui.main.map.viewport.ViewportLoadResult
import com.peto.ramap.ui.main.map.viewport.ViewportShopLoader
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect.ShowToast
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.apple_login_failure_message
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.filter_empty_visible_result_message
import ramap.shared.generated.resources.hidden_shop_search_result_message
import ramap.shared.generated.resources.kakao_login_failure_message
import ramap.shared.generated.resources.location_permission_enable_message
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.login_success_message
import ramap.shared.generated.resources.search_result_empty_message
import ramap.shared.generated.resources.search_result_hidden_only_message
import kotlin.time.Clock

class MapViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val searchRamenShopsUseCase: SearchRamenShopsUseCase,
    private val loginRepository: LoginRepository,
    private val currentLocationStore: CurrentLocationStore,
    private val personalizationStore: PersonalizationStore,
    private val mapSearchHistoryStorage: SearchHistoryStorage,
    private val mapAnalytics: MapAnalytics,
    private val operatingNoticeRepository: OperatingNoticeRepository,
    private val signInUseCase: SignInUseCase,
) : BaseViewModel<MapUiState, MapIntent, MapSideEffect>(initialState = MapUiState()) {
    private val viewportShopLoader = ViewportShopLoader(ramenShopRepository, viewModelScope)
    private var pendingBookmarkedView = false
    private var failedShopId: String? = null
    private var shopFocusSequence = 0L

    init {
        viewModelScope.launch { observeSessionState() }
        viewModelScope.launch { observePersonalization() }
        viewModelScope.launch { observeRecentSearches() }
        viewModelScope.launch { observeRecentlyViewedShops() }
        loadOperatingNotices()
    }

    private fun loadOperatingNotices() {
        launchResultTask(
            taskKey = OPERATING_NOTICES_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            retryOnNetworkError = true,
            request = operatingNoticeRepository::fetchCurrentOperatingNotices,
            onSuccess = { notices -> reduce { copy(operatingNotices = notices) } },
        )
    }

    private suspend fun observeRecentSearches() {
        mapSearchHistoryStorage.recentSearches.collectLatest { searches ->
            reduce { copy(recentSearches = searches) }
        }
    }

    private suspend fun observeRecentlyViewedShops() {
        val shopIds = mapSearchHistoryStorage.recentlyViewedShopIds.first()
        if (shopIds.isEmpty()) return
        val shops =
            when (val result = ramenShopRepository.fetchRamenShops(shopIds.toSet())) {
                is RamapResult.Success -> RamenShops(shopIds.mapNotNull { result.data[it] })
                is RamapResult.Error -> RamenShops(emptyMap())
            }
        reduce { copy(recentlyViewedShops = shops) }
    }

    private suspend fun observePersonalization() {
        personalizationStore.state.collectLatest { state ->
            val personalization = (state as? PersonalizationBootstrapState.Success)?.value ?: return@collectLatest
            if (!currentState.isLoggedIn) return@collectLatest
            loadPersonalizedShops(personalization.bookmarkedShopIds + personalization.hiddenShopIds)
            reduce {
                copy(
                    selectedShop = if (selectedShop?.id in hiddenShopIds && selectedShop?.id !in personalization.hiddenShopIds) selectedShop?.copy(isVisible = true) else selectedShop,
                    bookmarkedShopIds = personalization.bookmarkedShopIds,
                    hiddenShopIds = personalization.hiddenShopIds,
                    notificationShopIds = personalization.notificationShopIds,
                )
            }
            resumeBookmarkedView()
        }
    }

    override suspend fun handleIntent(intent: MapIntent) {
        when {
            handleViewportIntent(intent) -> Unit
            handleShopIntent(intent) -> Unit
            handleSearchIntent(intent) -> Unit
            else ->
                when (intent) {
                    OnBookmarkedShopsToggled -> toggleBookmarkedView()
                    is MapIntent.OnLoginTypeSelected -> login(intent.type)
                    MapIntent.OnLoginSelectionDismissed -> pendingBookmarkedView = false
                    OnLocationPermissionBlocked -> showLocationPermissionBlockedToast()
                    is MapIntent.OnShopLoadFailed -> if (currentState.selectedShopId == intent.shopId) failedShopId = intent.shopId
                    is MapIntent.OnShopViewed -> recordRecentlyViewedShop(intent.shop)
                    MapIntent.OnSelectedShopHidden -> {
                        dismissShopDetail()
                        reduce { copy(search = search.dismissResults()) }
                    }
                    else -> error("Unhandled map intent: $intent")
                }
        }
    }

    private fun handleViewportIntent(intent: MapIntent): Boolean {
        when (intent) {
            is OnBoundsChanged -> scheduleRamenShopsLoad(intent.bounds)

            OnViewportLoadRetry -> scheduleRamenShopsLoad(currentState.bounds)

            is OnCameraPositionChanged -> updateCameraPosition(intent.position)

            is OnMyLocationChanged -> updateMyLocation(intent.location)

            OnInitialLocationFocusConsumed -> consumeInitialLocationFocus()

            is OnSelectedShopFocusConsumed -> consumeSelectedShopFocus(intent.request)

            OnMapTabExited -> dismissBottomSheet()

            OnOperatingNoticesRefreshRequested -> loadOperatingNotices()

            OnStatusTimeRefreshed -> reduce { copy(currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul"))) }

            else -> return false
        }
        return true
    }

    private fun dismissBottomSheet() {
        cancelTask(SEARCH_TASK_KEY)
        reduce { copy(search = search.reset()) }
        dismissShopDetail()
    }

    private fun handleShopIntent(intent: MapIntent): Boolean {
        when (intent) {
            is OnShopSelected -> {
                mapAnalytics.logShopSelected(intent.shop, intent.source)
                selectShop(intent.shop, intent.shouldFocus)
            }
            is OnShopIdSelected -> selectShop(intent.shopId, intent.showDetail)
            is OnShopNavigationRequested -> openShopFromNavigation(intent.shopId, intent.showDetail)
            OnRequestedShopDismissed, OnShopDetailDismissed -> {
                reduce { copy(search = if (search.results.size == 1) search.dismissResults() else search) }
                dismissShopDetail()
            }
            else -> return false
        }
        return true
    }

    private suspend fun handleSearchIntent(intent: MapIntent): Boolean {
        when (intent) {
            is OnSearchResultsDismissed -> dismissSearchResults()
            is OnQueryChanged -> updateQuery(intent.query, clearFilters = true)
            is OnRecentSearchSelected -> updateQuery(intent.query)
            is OnRecentSearchDeleted -> mapSearchHistoryStorage.removeRecentSearch(intent.query)
            OnRecentSearchesCleared -> mapSearchHistoryStorage.clearRecentSearches()
            is OnCategoryFilterToggled -> toggleCategoryFilter(intent.category)
            OnOpenFilterToggled -> toggleOpenFilter()
            else -> return false
        }
        return true
    }

    private suspend fun observeSessionState() {
        loginRepository.sessionState.collectLatest { sessionState ->
            val isAuthenticated = sessionState == LoginSessionState.AUTHENTICATED
            updateAuthState(isAuthenticated)
            if (isAuthenticated) resumeBookmarkedView()
        }
    }

    private fun updateAuthState(isAuthenticated: Boolean) {
        reduce {
            copy(
                isLoggedIn = isAuthenticated,
                bookmarkedShopIds = if (isAuthenticated) bookmarkedShopIds else emptySet(),
                notificationShopIds = if (isAuthenticated) notificationShopIds else emptySet(),
                hiddenShopIds = if (isAuthenticated) hiddenShopIds else emptySet(),
                isBookmarkedView = isAuthenticated && isBookmarkedView,
            )
        }
    }

    private fun selectShop(
        shop: RamenShop,
        shouldFocus: Boolean = true,
    ) {
        if (isCurrentShopDetail(shop.id)) return
        failedShopId = null
        val focusRequest = if (shouldFocus) ShopFocusRequest(shop.id, ++shopFocusSequence) else null
        reduce {
            copy(
                selectedShopFocusRequest = focusRequest,
                search = search.consumeResultFocus(shop.id in search.results),
            )
        }
        pendingBookmarkedView = false
        reduce { copy(selectedShopId = shop.id, selectedShop = shop, shopSelectionKey = shopSelectionKey + 1) }
    }

    private fun selectShop(
        shopId: String,
        showDetail: Boolean = true,
    ) {
        if (shopId.isBlank() || isCurrentShopDetail(shopId)) return
        failedShopId = null
        consumeInitialLocationFocus()
        val focusRequest = ShopFocusRequest(shopId, ++shopFocusSequence)
        reduce { copy(selectedShopFocusRequest = focusRequest) }
        pendingBookmarkedView = false
        reduce { copy(selectedShopId = shopId, selectedShop = null, shopSelectionKey = shopSelectionKey + 1) }
        if (!showDetail) loadSelectedShopLocation(shopId)
    }

    private fun openShopFromNavigation(
        shopId: String,
        showDetail: Boolean,
    ) {
        if (shopId.isBlank()) return
        cancelTask(SEARCH_TASK_KEY)
        reduce { copy(search = search.reset(), filters = RamenShopFilter(), isBookmarkedView = false) }
        dismissShopDetail()
        selectShop(shopId, showDetail)
    }

    private fun isCurrentShopDetail(shopId: String): Boolean = currentState.selectedShopId == shopId && failedShopId != shopId

    private fun updateMyLocation(location: Location) {
        currentLocationStore.update(location)
        reduce {
            copy(
                currentLocation = location,
                locationFocusStatus = locationFocusStatus.request(location),
            )
        }
    }

    private fun consumeInitialLocationFocus() {
        reduce { copy(locationFocusStatus = LocationFocusStatus.Consumed) }
    }

    private fun consumeSelectedShopFocus(request: ShopFocusRequest) {
        reduce {
            if (selectedShopFocusRequest == request && shouldFocusSelectedShop) {
                copy(selectedShopFocusRequest = null)
            } else {
                this
            }
        }
    }

    private fun updateCameraPosition(position: CameraPosition) {
        reduce {
            copy(
                cameraPosition = position,
            )
        }
    }

    private fun dismissSearchResults() {
        reduce { copy(search = search.dismissResults()) }
    }

    private fun updateQuery(
        query: String,
        clearFilters: Boolean = false,
    ) {
        val normalizedQuery = SearchQuery(query).normalizeShopSearchQuery()
        updateSearchInput(query, clearFilters)

        if (!normalizedQuery.isValidForShopSearch()) {
            clearSearchResultsForInvalidQuery()
            return
        }

        searchShops(normalizedQuery)
    }

    private fun clearSearchResultsForInvalidQuery() {
        cancelTask(SEARCH_TASK_KEY)
        clearSearchResults()
    }

    private fun searchShops(query: SearchQuery) {
        if (canReuseSearchResults(query)) {
            cancelTask(SEARCH_TASK_KEY)
            recordRecentSearch(query.value)
            handleSingleSearchResult(currentState.searchResultShops.singleShopOrNull())
            return
        }

        launchTask(
            taskKey = SEARCH_TASK_KEY,
            loadKey = MapLoadKey.Search,
            policy = TaskPolicy.CancelPrevious,
        ) {
            val result = searchRamenShopsUseCase(query)
            if (currentCoroutineContext().isActive) handleSearchResult(query, result)
        }
    }

    private fun canReuseSearchResults(query: SearchQuery): Boolean =
        query.value.isNotBlank() &&
            currentState.search.hasLoadedResultsFor(query) &&
            currentState.search.results.isNotEmpty()

    private fun updateSearchInput(
        query: String,
        clearFilters: Boolean,
    ) {
        dismissShopDetail()
        reduce {
            copy(
                search = search.updateInput(query),
                filters = if (clearFilters) filters.clear() else filters,
                isBookmarkedView = if (clearFilters) false else isBookmarkedView,
            )
        }
    }

    private fun toggleCategoryFilter(category: Category) {
        val currentFilter = currentState.filters
        val nextFilter =
            if (category in currentFilter) {
                currentFilter - category
            } else {
                currentFilter + category
            }
        mapAnalytics.logCategoryFilterToggled(category, category !in currentFilter)
        updateFilter(nextFilter)
    }

    private fun toggleOpenFilter() {
        val filter = currentState.filters
        updateFilter(filter.copy(isOpenSelected = !filter.isOpenSelected))
    }

    private fun loadPersonalizedShops(shopIds: Set<String>) {
        if (shopIds.isEmpty()) return

        launchResultTask(
            taskKey = PERSONALIZED_SHOPS_TASK_KEY,
            retryOnNetworkError = true,
            request = { ramenShopRepository.fetchRamenShops(shopIds) },
            onSuccess = ::mergePersonalizedShops,
        )
    }

    private fun mergePersonalizedShops(result: RamenShops) {
        reduce { copy(shops = RamenShops(shops + result)) }
    }

    private fun toggleBookmarkedView() {
        if (currentState.isLoggedIn) {
            updateBookmarkedView()
        } else {
            dismissShopDetail()
            pendingBookmarkedView = true
            showInteraction(ShopInteractionEffect.ShowLoginGuide)
        }
    }

    private fun updateBookmarkedView() {
        val enabled = !currentState.isBookmarkedView
        if (!shouldKeepSelectedShop(currentState, enabled)) dismissShopDetail()
        reduce { copy(isBookmarkedView = enabled) }
    }

    private fun shouldKeepSelectedShop(
        state: MapUiState,
        willShowBookmarkedShops: Boolean,
    ): Boolean {
        val shop = state.selectedShop ?: return true
        return if (willShowBookmarkedShops) {
            shop.id in state.bookmarkedShopIds
        } else {
            shop.id !in state.hiddenShopIds
        }
    }

    private fun updateFilter(filter: RamenShopFilter) {
        val selectedShop = currentState.selectedShop
        val shouldDismissShop =
            selectedShop != null &&
                !currentState.shops
                    .filterByOpenStatus(
                        filter,
                        Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul")),
                        currentState.operatingNotices,
                    ).containsKey(selectedShop.id)
        if (shouldDismissShop) {
            dismissShopDetail()
        }
        reduce { copy(filters = filter) }
        showEmptyFilterResultMessageIfNeeded(includeSelectedShop = !shouldDismissShop)
    }

    private fun showEmptyFilterResultMessageIfNeeded(includeSelectedShop: Boolean = true) {
        val state = currentState
        if (state.filters.isEmpty() || state.markerShops(includeSelectedShop).hasVisibleShopIn(state.bounds)) return

        showToast(Res.string.filter_empty_visible_result_message)
    }

    private fun clearSearchResults() {
        reduce {
            copy(
                search = search.clearResults(),
            )
        }
    }

    private fun handleSearchResult(
        query: SearchQuery,
        result: RamapResult<RamenShops>,
    ) {
        when (result) {
            is RamapResult.Success -> handleLoadedSearchResult(query, result.data)
            is RamapResult.Error -> handleFailedSearchResult(result.error)
        }
    }

    private fun handleLoadedSearchResult(
        query: SearchQuery,
        shops: RamenShops,
    ) {
        recordRecentSearch(query.value)
        reduceSearchResult(query, shops)
        val searchResultShops = currentState.searchResultShops
        // 검색시 필터 적용으로 인해 검색 결과가 없을 때
        if (searchResultShops.isEmpty() && currentState.filters.isNotEmpty()) {
            showToast(Res.string.filter_empty_visible_result_message)
        }
        // 검색 결과에 숨김 매장이 포함되어 있을 때
        if (searchResultShops.size > 1 && searchResultShops.values.any { !it.isVisible }) {
            showToast(Res.string.search_result_hidden_only_message)
        }
        if (shops.isNotEmpty()) {
            handleSingleSearchResult(searchResultShops.singleShopOrNull())
            return
        }
        // 검색 결과가 전혀 없을 때
        showToast(Res.string.search_result_empty_message)
        reduce { copy(search = search.reset()) }
    }

    private fun handleFailedSearchResult(error: RamapError) {
        handleError(error)
        val searchErrorToast = PlaceSearchErrorToast.resolve(error)
        if (searchErrorToast != null) {
            showInteraction(ShowToast(searchErrorToast))
            return
        }
        showDataLoadFailure()
    }

    private fun showDataLoadFailure() {
        showToast(Res.string.data_load_failure_message, ToastType.ERROR)
    }

    private fun handleSingleSearchResult(shop: RamenShop?) {
        when {
            shop == null -> Unit
            shop.isVisible -> selectShop(shop)
            else -> showToast(Res.string.hidden_shop_search_result_message)
        }
    }

    private fun showToast(
        messageResource: StringResource,
        type: ToastType = ToastType.DEFAULT,
    ) {
        showInteraction(ShowToast(ToastData(messageResource, type)))
    }

    private fun showLocationPermissionBlockedToast() {
        showInteraction(ShowToast(locationPermissionBlockedToastData()))
    }

    private fun locationPermissionBlockedToastData(): ToastData =
        ToastData(
            message = Res.string.location_permission_enable_message,
            type = ToastType.DEFAULT,
            action = ToastAction(label = Res.string.location_permission_settings_action),
        )

    private fun reduceSearchResult(
        query: SearchQuery,
        result: RamenShops,
    ) {
        dismissShopDetail()
        reduce { copy(search = search.updateResults(query, result)) }
    }

    private fun recordRecentSearch(query: String) {
        viewModelScope.launch { mapSearchHistoryStorage.addRecentSearch(query) }
    }

    private fun recordRecentlyViewedShop(shop: RamenShop) {
        if (currentState.selectedShopId != shop.id) return
        failedShopId = null
        reduce {
            copy(
                selectedShop = shop,
                recentlyViewedShops =
                    RamenShops(
                        listOf(shop) + recentlyViewedShops.values.filterNot { it.id == shop.id },
                    ),
            )
        }
    }

    private fun scheduleRamenShopsLoad(bounds: MapBounds) {
        reduce {
            copy(
                bounds = bounds,
                hasViewportLoadFailed = false,
            )
        }
        viewportShopLoader.schedule(
            bounds = bounds,
            onResult = ::handleViewportLoadResult,
        )
    }

    private fun handleViewportLoadResult(result: ViewportLoadResult) {
        when (result) {
            is ViewportLoadResult.Loaded -> handleLoadedViewport(result)
            is ViewportLoadResult.Failed -> handleFailedViewport(result)
        }
    }

    private fun handleLoadedViewport(result: ViewportLoadResult.Loaded) {
        val mergedShops = mergeShops(result.shops)
        reduce {
            if (shops == mergedShops && !hasViewportLoadFailed) {
                this
            } else {
                copy(shops = mergedShops, hasViewportLoadFailed = false)
            }
        }
    }

    private fun handleFailedViewport(result: ViewportLoadResult.Failed) {
        mapAnalytics.logViewportLoadError()
        handleError(result.error)
        reduce { copy(hasViewportLoadFailed = true) }
    }

    private fun mergeShops(newShops: RamenShops): RamenShops =
        RamenShops(
            currentState.shops + newShops,
        )

    private fun dismissShopDetail() {
        cancelTask(SELECTED_SHOP_TASK_KEY)
        pendingBookmarkedView = false
        reduce { copy(selectedShopId = null, selectedShop = null, selectedShopFocusRequest = null) }
    }

    private fun loadSelectedShopLocation(shopId: String) {
        val selectionKey = currentState.shopSelectionKey
        launchResultTask(
            taskKey = SELECTED_SHOP_TASK_KEY,
            request = { ramenShopRepository.fetchRamenShops(setOf(shopId)) },
            onSuccess = { shops ->
                if (currentCoroutineContext().isActive && currentState.shopSelectionKey == selectionKey && currentState.selectedShopId == shopId) {
                    shops[shopId]?.let { shop ->
                        recordRecentlyViewedShop(shop)
                        mapSearchHistoryStorage.addRecentlyViewedShop(shopId)
                    }
                }
            },
            onError = { showInteraction(ShowToast(ToastData(Res.string.data_load_failure_message, ToastType.ERROR))) },
        )
    }

    private fun resumeBookmarkedView() {
        if (!pendingBookmarkedView || !currentState.isLoggedIn) return
        val personalization = (personalizationStore.state.value as? PersonalizationBootstrapState.Success)?.value ?: return
        pendingBookmarkedView = false
        reduce { copy(bookmarkedShopIds = personalization.bookmarkedShopIds, hiddenShopIds = personalization.hiddenShopIds, notificationShopIds = personalization.notificationShopIds) }
        updateBookmarkedView()
    }

    private fun login(type: LoginType) {
        launchResultTask(
            taskKey = "map-login",
            request = { signInUseCase(type, AnalyticsSource.MAP) },
            onSuccess = { showInteraction(ShowToast(ToastData(Res.string.login_success_message, ToastType.DEFAULT))) },
            onError = {
                val message =
                    when (type) {
                        LoginType.KAKAO -> Res.string.kakao_login_failure_message
                        LoginType.APPLE -> Res.string.apple_login_failure_message
                    }
                showInteraction(ShowToast(ToastData(message, ToastType.ERROR)))
            },
        )
    }

    private fun showInteraction(effect: ShopInteractionEffect) {
        trySideEffect(MapSideEffect.Interaction(effect))
    }

    companion object {
        private const val SELECTED_SHOP_TASK_KEY = "map-selected-shop"
        private const val SEARCH_TASK_KEY = "map-search"
        private const val PERSONALIZED_SHOPS_TASK_KEY = "map-personalized-shops"
        private const val OPERATING_NOTICES_TASK_KEY = "map-operating-notices"
    }
}
