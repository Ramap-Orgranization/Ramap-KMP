package com.peto.ramap.ui.main.map

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.analytics.common.login.LoginMethod
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.shop.model.ShopDetailSheetUiState
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.report.ShopInformationReport
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShopFilter
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.domain.usecase.ShopDetailCacheLookup
import com.peto.ramap.platform.storage.SearchHistoryStorage
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.location.CurrentLocationStore
import com.peto.ramap.ui.main.map.contract.MapIntent
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkedShopsToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBoundsChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCameraPositionChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCategoryFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnHiddenToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnInitialLocationFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLocationPermissionBlocked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLoginTypeSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMapTabExited
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMyLocationChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnOpenFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnQueryChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchDeleted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchesCleared
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRequestedShopDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSearchResultsDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSelectedShopFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailRetry
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopIdSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopMapLinkClicked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopNotificationToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopReportSubmitted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopShareClicked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnViewportLoadRetry
import com.peto.ramap.ui.main.map.contract.MapLoadKey
import com.peto.ramap.ui.main.map.contract.MapSideEffect
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShareShop
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowLoginGuide
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowToast
import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.main.map.log.MapAnalytics
import com.peto.ramap.ui.main.map.model.CameraPosition
import com.peto.ramap.ui.main.map.model.PendingMapAction
import com.peto.ramap.ui.main.map.model.location.LocationFocusStatus
import com.peto.ramap.ui.main.map.viewport.ViewportLoadResult
import com.peto.ramap.ui.main.map.viewport.ViewportShopLoader
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.apple_login_failure_message
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.filter_empty_visible_result_message
import ramap.shared.generated.resources.hidden_shop_notification_unavailable_message
import ramap.shared.generated.resources.hidden_shop_search_result_message
import ramap.shared.generated.resources.hide_shop_success_message
import ramap.shared.generated.resources.kakao_login_failure_message
import ramap.shared.generated.resources.location_permission_enable_message
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.login_success_message
import ramap.shared.generated.resources.personalization_update_failure_message
import ramap.shared.generated.resources.search_result_empty_message
import ramap.shared.generated.resources.search_result_hidden_only_message
import ramap.shared.generated.resources.shop_information_report_failure_message
import ramap.shared.generated.resources.shop_information_report_success_message
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class MapViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val loginRepository: LoginRepository,
    private val currentLocationStore: CurrentLocationStore,
    private val shopReportRepository: ShopReportRepository,
    private val personalizationStore: ShopPersonalizationStore,
    private val fetchShopDetailUseCase: FetchShopDetailUseCase,
    private val mapSearchHistoryStorage: SearchHistoryStorage,
    private val mapAnalytics: MapAnalytics,
    private val loginAnalytics: LoginAnalytics,
) : BaseViewModel<MapUiState, MapIntent, MapSideEffect>(initialState = MapUiState()) {
    private val viewportShopLoader = ViewportShopLoader(ramenShopRepository, viewModelScope)
    private var pendingMapAction: PendingMapAction? = null
    private var observedBookmarkedShopIds: Set<String>? = null
    private var openFilterRefreshJob: Job? = null

    init {
        viewModelScope.launch { observeSessionState() }
        viewModelScope.launch { observePersonalization() }
        viewModelScope.launch { observeRecentSearches() }
        viewModelScope.launch { observeRecentlyViewedShops() }
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
            val personalization =
                (state as? PersonalizationBootstrapState.Success)?.value
                    ?: return@collectLatest
            if (!currentState.isLoggedIn) return@collectLatest
            updatePersonalization(personalization)
            synchronizeShopDetailLikeCounts(personalization.bookmarkedShopIds)
            resumePendingAction()
        }
    }

    override suspend fun handleIntent(intent: MapIntent) {
        when {
            handleViewportIntent(intent) -> Unit
            handleShopIntent(intent) -> Unit
            handleSearchIntent(intent) -> Unit
            handlePersonalizationIntent(intent) -> Unit
            else -> handleAccountIntent(intent)
        }
    }

    private fun handleViewportIntent(intent: MapIntent): Boolean {
        when (intent) {
            is OnBoundsChanged -> scheduleRamenShopsLoad(intent.bounds)

            OnViewportLoadRetry -> scheduleRamenShopsLoad(currentState.bounds)

            is OnCameraPositionChanged -> updateCameraPosition(intent.position)

            is OnMyLocationChanged -> updateMyLocation(intent.location)

            OnInitialLocationFocusConsumed -> consumeInitialLocationFocus()

            OnSelectedShopFocusConsumed -> consumeSelectedShopFocus()

            OnMapTabExited -> dismissBottomSheet()

            else -> return false
        }
        return true
    }

    private fun dismissBottomSheet() {
        cancelShopDetailLoad()
        cancelTask(SEARCH_TASK_KEY)
        reduce {
            copy(
                shopDetailState = ShopDetailSheetUiState.Closed,
                search = search.reset(),
            )
        }
    }

    private fun handleShopIntent(intent: MapIntent): Boolean {
        when (intent) {
            is OnShopSelected -> {
                mapAnalytics.logShopSelected(intent.shop, intent.source)
                selectShop(intent.shop, intent.shouldFocus)
            }

            is OnShopIdSelected -> selectShop(intent.shopId)
            is OnShopShareClicked -> shareShop(intent.shop)
            is OnShopMapLinkClicked ->
                mapAnalytics.logShopMapLinkOpened(
                    intent.shop,
                    intent.mapProvider,
                )

            OnRequestedShopDismissed -> dismissRequestedShopLoad()
            is OnShopDetailDismissed -> dismissShopDetail()
            OnShopDetailRetry -> retryShopDetailLoad()
            else -> return false
        }
        return true
    }

    private fun shareShop(shop: RamenShop) {
        mapAnalytics.logShopShared(shop)
        trySideEffect(ShareShop(shop.id, shop.name))
    }

    private suspend fun handleSearchIntent(intent: MapIntent): Boolean {
        when (intent) {
            is OnSearchResultsDismissed -> dismissSearchResults()
            is OnQueryChanged -> updateQuery(intent.query)
            is OnRecentSearchSelected -> updateQuery(intent.query)
            is OnRecentSearchDeleted -> mapSearchHistoryStorage.removeRecentSearch(intent.query)
            OnRecentSearchesCleared -> mapSearchHistoryStorage.clearRecentSearches()
            is OnCategoryFilterToggled -> toggleCategoryFilter(intent.category)
            OnOpenFilterToggled -> toggleOpenFilter()
            else -> return false
        }
        return true
    }

    private fun handlePersonalizationIntent(intent: MapIntent): Boolean {
        when (intent) {
            OnBookmarkedShopsToggled -> toggleBookmarkedView()
            is OnBookmarkToggled -> toggleBookmark(intent.shop, intent.source)
            is OnShopNotificationToggled -> toggleShopSubscribed(intent.shop, intent.source)
            is OnHiddenToggled -> toggleHidden(intent.shop, intent.source)
            is OnShopReportSubmitted ->
                submitShopInformationReport(
                    wrongFields = intent.wrongFields,
                    description = intent.description,
                )

            else -> return false
        }
        return true
    }

    private fun handleAccountIntent(intent: MapIntent) {
        when (intent) {
            is OnLoginTypeSelected -> {
                when (intent.type) {
                    LoginType.KAKAO -> signInWithKakao()
                    LoginType.APPLE -> signInWithApple()
                }
            }

            OnLocationPermissionBlocked -> showLocationPermissionBlockedToast()

            else -> error("Unhandled map intent: $intent")
        }
    }

    private suspend fun observeSessionState() {
        loginRepository.sessionState.collectLatest { sessionState ->
            val isAuthenticated = sessionState == LoginSessionState.AUTHENTICATED
            updateAuthState(isAuthenticated)
        }
    }

    private fun updateAuthState(isAuthenticated: Boolean) {
        if (!isAuthenticated) observedBookmarkedShopIds = null
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
        val cache = checkCachedShopDetail(shop.id)
        if (cache != null) recordRecentlyViewedShop(shop)
        val selectedShopState = createSelectedShopState(currentState, shop, shouldFocus, cache)
        reduce { selectedShopState }
        loadShopDetail(shop.id)
    }

    private fun createSelectedShopState(
        state: MapUiState,
        shop: RamenShop,
        shouldFocus: Boolean,
        cache: ShopDetail?,
    ): MapUiState =
        state.copy(
            shopDetailState = createSelectedShopDetailState(shop, cache),
            shouldFocusSelectedShop = shouldFocus,
            shopWaiting =
                cache?.let { state.shopWaiting + (shop.id to it.waitingSystem) }
                    ?: state.shopWaiting,
            search = state.search.consumeResultFocus(shop.id in state.search.results),
        )

    private fun createSelectedShopDetailState(
        shop: RamenShop,
        cache: ShopDetail?,
    ): ShopDetailSheetUiState =
        cache?.let { detail ->
            ShopDetailSheetUiState.Content(
                detail.copy(shop = detail.shop.copy(isVisible = shop.isVisible)),
            )
        } ?: ShopDetailSheetUiState.Loading(shop.id, shop)

    private fun checkCachedShopDetail(shopId: String): ShopDetail? =
        when (val lookup = fetchShopDetailUseCase.findCached(shopId)) {
            is ShopDetailCacheLookup.Hit -> {
                cancelTask(SHOP_DETAIL_TASK_KEY)
                lookup.detail
            }

            ShopDetailCacheLookup.Miss -> null
        }

    private fun dismissShopDetail() {
        cancelShopDetailLoad()
        reduce {
            copy(
                shopDetailState = ShopDetailSheetUiState.Closed,
                search = if (search.results.size == 1) search.dismissResults() else search,
            )
        }
    }

    private fun retryShopDetailLoad() {
        val errorState = currentState.shopDetailState as? ShopDetailSheetUiState.Error ?: return
        loadShopDetail(
            shopId = errorState.shopId,
            selectShopOnSuccess = errorState.shop == null,
        )
    }

    private fun selectShop(shopId: String) {
        if (shopId.isBlank()) return
        consumeInitialLocationFocus()
        when (val lookup = fetchShopDetailUseCase.findCached(shopId)) {
            is ShopDetailCacheLookup.Hit -> {
                cancelShopDetailLoad()
                applyRequestedShopDetail(lookup.detail)
                return
            }

            ShopDetailCacheLookup.Miss -> Unit
        }

        loadShopDetail(shopId, selectShopOnSuccess = true)
    }

    private fun dismissRequestedShopLoad() {
        dismissShopDetail()
    }

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

    private fun consumeSelectedShopFocus() {
        reduce { copy(shouldFocusSelectedShop = false) }
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

    private fun updateQuery(query: String) {
        val normalizedQuery = SearchQuery(query).normalizeShopSearchQuery()
        updateSearchInput(query)

        if (normalizedQuery.value.length !in SEARCH_QUERY_LENGTH) {
            clearSearchResultsForInvalidQuery()
            return
        }

        recordRecentSearch(normalizedQuery.value)
        searchShops(normalizedQuery)
    }

    private fun clearSearchResultsForInvalidQuery() {
        cancelTask(SEARCH_TASK_KEY)
        clearSearchResults()
    }

    private fun searchShops(query: SearchQuery) {
        if (canReuseSearchResults(query)) {
            cancelTask(SEARCH_TASK_KEY)
            handleSingleSearchResult(currentState.searchResultShops.singleShopOrNull())
            return
        }

        launchTask(
            taskKey = SEARCH_TASK_KEY,
            loadKey = MapLoadKey.Search,
            policy = TaskPolicy.CancelPrevious,
        ) {
            val result = ramenShopRepository.searchRamenShops(query, SEARCH_RESULT_LIMIT)
            handleSearchResult(query, result)
        }
    }

    private fun canReuseSearchResults(query: SearchQuery): Boolean =
        query.value.isNotBlank() &&
            currentState.search.hasLoadedResultsFor(query) &&
            currentState.search.results.isNotEmpty()

    private fun updateSearchInput(query: String) {
        cancelShopDetailLoad()
        reduce {
            copy(
                search = search.updateInput(query),
                shopDetailState = ShopDetailSheetUiState.Closed,
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

    private suspend fun updatePersonalization(personalization: ShopPersonalization) {
        val hiddenShopIds = personalization.hiddenShopIds
        val bookmarkedShopIds = personalization.bookmarkedShopIds

        loadPersonalizedShops(bookmarkedShopIds + hiddenShopIds)
        applyPersonalization(personalization)
    }

    private fun applyPersonalization(personalization: ShopPersonalization) {
        reduce {
            copy(
                bookmarkedShopIds = personalization.bookmarkedShopIds,
                hiddenShopIds = personalization.hiddenShopIds,
                notificationShopIds = personalization.notificationShopIds,
                search = personalizedSearchState(this, personalization.hiddenShopIds),
                shopDetailState = personalizedDetailState(this, personalization.hiddenShopIds),
            )
        }
    }

    private fun personalizedSearchState(
        state: MapUiState,
        hiddenShopIds: Set<String>,
    ) = if (selectedShopBecameHidden(state, hiddenShopIds)) {
        state.search.dismissResults()
    } else {
        state.search
    }

    private fun personalizedDetailState(
        state: MapUiState,
        hiddenShopIds: Set<String>,
    ): ShopDetailSheetUiState {
        val becameHidden = selectedShopBecameHidden(state, hiddenShopIds)
        val becameVisible = selectedShopBecameVisible(state, hiddenShopIds)
        return updatedDetailAfterPersonalization(
            state.shopDetailState,
            becameHidden,
            becameVisible,
        )
    }

    private fun selectedShopBecameHidden(
        state: MapUiState,
        hiddenShopIds: Set<String>,
    ): Boolean = state.selectedShop?.id !in state.hiddenShopIds && state.selectedShop?.id in hiddenShopIds

    private fun selectedShopBecameVisible(
        state: MapUiState,
        hiddenShopIds: Set<String>,
    ): Boolean = state.selectedShop?.id in state.hiddenShopIds && state.selectedShop?.id !in hiddenShopIds

    private fun updatedDetailAfterPersonalization(
        state: ShopDetailSheetUiState,
        becameHidden: Boolean,
        becameVisible: Boolean,
    ): ShopDetailSheetUiState =
        when {
            becameHidden -> ShopDetailSheetUiState.Closed
            becameVisible -> detailStateWithVisibleShop(state)
            else -> state
        }

    private fun detailStateWithVisibleShop(state: ShopDetailSheetUiState): ShopDetailSheetUiState =
        when (state) {
            ShopDetailSheetUiState.Closed -> state
            is ShopDetailSheetUiState.Loading -> state.copy(shop = state.shop?.copy(isVisible = true))
            is ShopDetailSheetUiState.Content -> visibleContentState(state)
            is ShopDetailSheetUiState.Error -> state.copy(shop = state.shop?.copy(isVisible = true))
        }

    private fun visibleContentState(state: ShopDetailSheetUiState.Content): ShopDetailSheetUiState.Content =
        state.copy(
            detail = state.detail.copy(shop = state.detail.shop.copy(isVisible = true)),
        )

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

    private fun toggleBookmark(
        shop: RamenShop,
        source: AnalyticsSource,
    ) {
        executeLoginRequiredAction(
            PendingMapAction.ToggleBookmark(
                shop = shop,
                source = source,
                enabled = !currentState.isLoggedIn || shop.id !in currentState.bookmarkedShopIds,
            ),
        )
    }

    private fun updateBookmark(
        shop: RamenShop,
        source: AnalyticsSource,
        enabled: Boolean = shop.id !in currentState.bookmarkedShopIds,
    ) {
        if ((shop.id in currentState.bookmarkedShopIds) == enabled) return

        mapAnalytics.logBookmarkToggled(shop, enabled, source)
        postBookmark(shop.id, enabled)
    }

    private fun toggleShopSubscribed(
        shop: RamenShop,
        source: AnalyticsSource,
    ) {
        executeLoginRequiredAction(PendingMapAction.ToggleShopNotification(shop, source))
    }

    private fun updateShopNotification(
        shop: RamenShop,
        source: AnalyticsSource,
    ) {
        val isHiddenShop = shop.id in currentState.hiddenShopIds

        if (isHiddenShop) {
            showToast(Res.string.hidden_shop_notification_unavailable_message)
            return
        }

        val wasEnabled = shop.id in currentState.notificationShopIds
        val enabled = !wasEnabled

        mapAnalytics.logShopSubscribed(shop, enabled, source)

        postShopNotification(shop.id, wasEnabled)
    }

    private fun postShopNotification(
        shopId: String,
        wasEnabled: Boolean,
    ) {
        val enabled = !wasEnabled
        launchResultTask(
            taskKey = shopNotificationTaskKey(shopId),
            policy = TaskPolicy.IgnoreNew,
            request = { personalizationStore.updateShopNotification(shopId, enabled) },
            onError = { showPersonalizationUpdateFailure() },
        )
    }

    private fun postBookmark(
        shopId: String,
        enabled: Boolean,
    ) {
        launchResultTask(
            taskKey = bookmarkTaskKey(shopId),
            policy = TaskPolicy.IgnoreNew,
            request = { personalizationStore.updateBookmark(shopId, enabled) },
            onSuccess = { loadBookmarkedShopIfNeeded(shopId, wasBookmarked = !enabled) },
            onError = { showPersonalizationUpdateFailure() },
        )
    }

    private fun synchronizeShopDetailLikeCounts(bookmarkedShopIds: Set<String>) {
        val previousShopIds = observedBookmarkedShopIds
        observedBookmarkedShopIds = bookmarkedShopIds
        if (previousShopIds == null) return

        for (shopId in bookmarkedShopIds - previousShopIds) {
            updateShopDetailLikeCount(shopId, enabled = true)
        }
        for (shopId in previousShopIds - bookmarkedShopIds) {
            updateShopDetailLikeCount(shopId, enabled = false)
        }
    }

    private fun updateShopDetailLikeCount(
        shopId: String,
        enabled: Boolean,
    ) {
        fetchShopDetailUseCase.updateCachedLikeCount(shopId, enabled)
        val detailState = currentState.shopDetailState as? ShopDetailSheetUiState.Content ?: return
        if (detailState.detail.shop.id != shopId) return

        val likeCountDelta = if (enabled) 1L else -1L
        reduce {
            copy(
                shopDetailState =
                    detailState.copy(
                        detail =
                            detailState.detail.copy(
                                likeCount = (detailState.detail.likeCount + likeCountDelta).coerceAtLeast(0L),
                            ),
                    ),
            )
        }
    }

    private suspend fun loadBookmarkedShopIfNeeded(
        shopId: String,
        wasBookmarked: Boolean,
    ) {
        if (!wasBookmarked) loadPersonalizedShops(setOf(shopId))
    }

    private fun toggleHidden(
        shop: RamenShop,
        source: AnalyticsSource,
    ) {
        executeLoginRequiredAction(PendingMapAction.ToggleHidden(shop, source))
    }

    private fun updateHiddenShop(
        shop: RamenShop,
        source: AnalyticsSource,
    ) {
        val wasHidden = shop.id in currentState.hiddenShopIds
        mapAnalytics.logHiddenToggled(shop, !wasHidden, source)

        if (wasHidden) {
            unhideShop(shop)
        } else {
            hideShop(shop)
        }
    }

    private fun hideShop(shop: RamenShop) {
        launchResultTask(
            taskKey = hiddenShopTaskKey(shop.id),
            policy = TaskPolicy.IgnoreNew,
            request = { personalizationStore.hideShop(shop.id) },
            onSuccess = { showHiddenShopSuccess() },
            onError = { showPersonalizationUpdateFailure() },
        )
    }

    private fun showHiddenShopSuccess() {
        showToast(Res.string.hide_shop_success_message)
    }

    private fun unhideShop(shop: RamenShop) {
        launchResultTask(
            taskKey = hiddenShopTaskKey(shop.id),
            policy = TaskPolicy.IgnoreNew,
            request = { personalizationStore.unhideShop(shop.id) },
            onError = { showPersonalizationUpdateFailure() },
        )
    }

    private fun showPersonalizationUpdateFailure() {
        showToast(Res.string.personalization_update_failure_message, ToastType.ERROR)
    }

    private fun toggleBookmarkedView() {
        executeLoginRequiredAction(PendingMapAction.ToggleBookmarkedShops)
    }

    private fun updateBookmarkedView() {
        val enabled = !currentState.isBookmarkedView

        reduce {
            copy(
                isBookmarkedView = enabled,
                shopDetailState =
                    shopDetailState.takeIf {
                        shouldKeepSelectedShop(
                            state = this,
                            willShowBookmarkedShops = enabled,
                        )
                    } ?: ShopDetailSheetUiState.Closed,
            )
        }
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

    private fun isLoggedInOrShowGuide(): Boolean {
        if (currentState.isLoggedIn) return true

        trySideEffect(ShowLoginGuide)
        return false
    }

    private fun executeLoginRequiredAction(action: PendingMapAction) {
        if (currentState.isLoggedIn) {
            performPendingAction(action)
            return
        }
        pendingMapAction = action
        isLoggedInOrShowGuide()
    }

    private fun performPendingAction(action: PendingMapAction) {
        when (action) {
            is PendingMapAction.ToggleBookmark -> updateBookmark(action.shop, action.source, action.enabled)
            is PendingMapAction.ToggleShopNotification -> updateShopNotification(action.shop, action.source)
            is PendingMapAction.ToggleHidden -> updateHiddenShop(action.shop, action.source)
            PendingMapAction.ToggleBookmarkedShops -> updateBookmarkedView()
        }
    }

    private fun submitShopInformationReport(
        wrongFields: Set<ShopInformationField>,
        description: String,
    ) {
        val report = createShopInformationReport(wrongFields, description) ?: return
        launchResultTask(
            taskKey = SHOP_REPORT_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = { shopReportRepository.submitShopInformationReport(report) },
            onSuccess = { showShopInformationReportSuccess() },
            onError = { showShopInformationReportFailure() },
        )
    }

    private fun createShopInformationReport(
        wrongFields: Set<ShopInformationField>,
        description: String,
    ): ShopInformationReport? {
        val shop = currentState.selectedShop ?: return null
        if (wrongFields.isEmpty() && description.isBlank()) return null
        return ShopInformationReport(
            shopId = shop.id,
            shopName = shop.name,
            wrongFields = wrongFields,
            description = description.trim(),
        )
    }

    private fun showShopInformationReportSuccess() {
        showToast(Res.string.shop_information_report_success_message)
    }

    private fun showShopInformationReportFailure() {
        showToast(Res.string.shop_information_report_failure_message, ToastType.ERROR)
    }

    private fun signInWithKakao() {
        loginAnalytics.logLoginStarted(AnalyticsSource.MAP)
        launchResultTask(
            taskKey = SIGN_IN_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = { loginRepository.signIn(LoginType.KAKAO) },
            onSuccess = {
                loginAnalytics.logLoginSucceeded(AnalyticsSource.MAP)
                showToast(Res.string.login_success_message)
            },
            onError = {
                loginAnalytics.logLoginFailed(AnalyticsSource.MAP)
                showKakaoLoginFailure()
            },
        )
    }

    private fun signInWithApple() {
        loginAnalytics.logLoginStarted(AnalyticsSource.MAP, LoginMethod.APPLE)
        launchResultTask(
            taskKey = SIGN_IN_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = { loginRepository.signIn(LoginType.APPLE) },
            onSuccess = {
                loginAnalytics.logLoginSucceeded(AnalyticsSource.MAP, LoginMethod.APPLE)
                showToast(Res.string.login_success_message)
            },
            onError = {
                loginAnalytics.logLoginFailed(AnalyticsSource.MAP, LoginMethod.APPLE)
                showAppleLoginFailure()
            },
        )
    }

    private fun showKakaoLoginFailure() {
        showToast(Res.string.kakao_login_failure_message, ToastType.ERROR)
    }

    private fun showAppleLoginFailure() {
        showToast(Res.string.apple_login_failure_message, ToastType.ERROR)
    }

    private fun resumePendingAction() {
        val pending = pendingMapAction ?: return
        pendingMapAction = null
        performPendingAction(pending)
    }

    private fun updateFilter(filter: RamenShopFilter) {
        reduce {
            copy(
                filters = filter,
                shopDetailState =
                    shopDetailState.takeIf {
                        selectedShop?.let { shop ->
                            currentState.shops
                                .filterByOpenStatus(
                                    filter,
                                    Clock.System
                                        .now()
                                        .toLocalDateTime(TimeZone.currentSystemDefault()),
                                ).containsKey(shop.id)
                        } ?: true
                    } ?: ShopDetailSheetUiState.Closed,
            )
        }
        updateOpenFilterRefreshJob(filter.isOpenSelected)
        showEmptyFilterResultMessageIfNeeded()
    }

    private fun updateOpenFilterRefreshJob(isEnabled: Boolean) {
        if (!isEnabled) {
            openFilterRefreshJob?.cancel()
            openFilterRefreshJob = null
            return
        }
        if (openFilterRefreshJob?.isActive == true) return

        openFilterRefreshJob =
            viewModelScope.launch {
                while (true) {
                    delay(OPEN_FILTER_REFRESH_INTERVAL_MILLIS.milliseconds)
                    refreshOpenFilter()
                }
            }
    }

    private fun refreshOpenFilter() {
        reduce {
            copy(
                openFilterRefreshVersion = openFilterRefreshVersion + 1,
                shopDetailState =
                    shopDetailState.takeIf {
                        selectedShop?.let { shop ->
                            currentState.shops
                                .filterByOpenStatus(
                                    filters,
                                    Clock.System
                                        .now()
                                        .toLocalDateTime(TimeZone.currentSystemDefault()),
                                ).containsKey(shop.id)
                        } ?: true
                    } ?: ShopDetailSheetUiState.Closed,
            )
        }
    }

    private fun showEmptyFilterResultMessageIfNeeded() {
        val state = currentState
        if (state.filters.isEmpty() || state.markerShops.hasVisibleShopIn(state.bounds)) return

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
        trySideEffect(ShowToast(ToastData(messageResource, type)))
    }

    private fun showLocationPermissionBlockedToast() {
        trySideEffect(ShowToast(locationPermissionBlockedToastData()))
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
        reduce {
            copy(
                search = search.updateResults(query, result),
                shopDetailState = ShopDetailSheetUiState.Closed,
            )
        }
    }

    /** 선택 매장이 바뀌면 이전 상세 조회를 교체하고 결과가 현재 선택 매장과 일치할 때만 반영한다. */
    private fun loadShopDetail(
        shopId: String,
        selectShopOnSuccess: Boolean = false,
    ) {
        launchTask(
            taskKey = SHOP_DETAIL_TASK_KEY,
            loadKey = MapLoadKey.ShopDetail,
            policy = TaskPolicy.CancelPrevious,
            onStart = {
                if (hasContentFor(shopId)) {
                    this
                } else {
                    createShopDetailLoadingState(this, shopId, selectShopOnSuccess)
                }
            },
        ) {
            handleShopDetailResult(shopId, selectShopOnSuccess, fetchShopDetailUseCase(shopId))
        }
    }

    private fun hasContentFor(shopId: String): Boolean =
        currentState.shopDetailState.let {
            it is ShopDetailSheetUiState.Content && it.detail.shop.id == shopId
        }

    private fun createShopDetailLoadingState(
        state: MapUiState,
        shopId: String,
        selectShopOnSuccess: Boolean,
    ): MapUiState {
        val shop = if (selectShopOnSuccess) null else state.selectedShop
        return state.copy(shopDetailState = ShopDetailSheetUiState.Loading(shopId, shop))
    }

    private fun handleShopDetailResult(
        shopId: String,
        selectShopOnSuccess: Boolean,
        result: RamapResult<ShopDetail>,
    ) {
        when (result) {
            is RamapResult.Success ->
                handleShopDetailLoadSuccess(
                    shopId,
                    selectShopOnSuccess,
                    result.data,
                )

            is RamapResult.Error ->
                handleShopDetailLoadError(
                    shopId,
                    selectShopOnSuccess,
                    result.error,
                )
        }
    }

    private fun handleShopDetailLoadSuccess(
        shopId: String,
        selectShopOnSuccess: Boolean,
        detail: ShopDetail,
    ) {
        if (selectShopOnSuccess) {
            applyRequestedShopDetail(detail)
            return
        }
        val selectedShop = resolveSelectedShop(shopId) ?: return
        handleShopDetailSuccess(detail, selectedShop)
    }

    /** Loading 또는 Content 상태에서 현재 shopId에 해당하는 매장을 찾는다. */
    private fun resolveSelectedShop(shopId: String): RamenShop? =
        when (val state = currentState.shopDetailState) {
            is ShopDetailSheetUiState.Loading -> state.shop?.takeIf { state.shopId == shopId }
            is ShopDetailSheetUiState.Content -> state.detail.shop.takeIf { it.id == shopId }
            else -> null
        }

    private fun handleShopDetailLoadError(
        shopId: String,
        selectShopOnSuccess: Boolean,
        error: RamapError,
    ) {
        // 캐시된 Content가 있으면 갱신 실패에도 기존 내용을 유지한다.
        if (currentState.shopDetailState is ShopDetailSheetUiState.Content) return
        val loadingState = currentLoadingDetail(shopId, selectShopOnSuccess) ?: return
        handleShopDetailFailure(error, loadingState)
    }

    /** 상세 UI가 닫히거나 검색 상태가 바뀔 때 진행 중 상세 조회와 로딩을 함께 정리한다. */
    private fun cancelShopDetailLoad() {
        cancelTask(SHOP_DETAIL_TASK_KEY)
    }

    private fun handleShopDetailSuccess(
        detail: ShopDetail,
        selectedShop: RamenShop,
    ) {
        val selectedDetail =
            detail.copy(shop = detail.shop.copy(isVisible = selectedShop.isVisible))
        reduce {
            copy(
                shopWaiting = shopWaiting + (selectedShop.id to detail.waitingSystem),
                shopDetailState = ShopDetailSheetUiState.Content(selectedDetail),
            )
        }
        recordRecentlyViewedShop(selectedDetail.shop)
    }

    private fun applyRequestedShopDetail(detail: ShopDetail) {
        reduce {
            copy(
                shouldFocusSelectedShop = true,
                shopWaiting = shopWaiting + (detail.shop.id to detail.waitingSystem),
                shopDetailState = ShopDetailSheetUiState.Content(detail),
            )
        }
        recordRecentlyViewedShop(detail.shop)
    }

    private fun recordRecentSearch(query: String) {
        viewModelScope.launch { mapSearchHistoryStorage.addRecentSearch(query) }
    }

    private fun recordRecentlyViewedShop(shop: RamenShop) {
        reduce {
            copy(
                recentlyViewedShops =
                    RamenShops(
                        listOf(shop) + recentlyViewedShops.values.filterNot { it.id == shop.id },
                    ),
            )
        }
        viewModelScope.launch { mapSearchHistoryStorage.addRecentlyViewedShop(shop.id) }
    }

    private fun handleShopDetailFailure(
        error: RamapError,
        loadingState: ShopDetailSheetUiState.Loading,
    ) {
        handleError(error)
        reduce {
            copy(
                shopDetailState =
                    ShopDetailSheetUiState.Error(
                        loadingState.shopId,
                        loadingState.shop,
                    ),
            )
        }
    }

    private fun currentLoadingDetail(
        shopId: String,
        expectsRequestedShop: Boolean,
    ): ShopDetailSheetUiState.Loading? {
        val state = currentState.shopDetailState as? ShopDetailSheetUiState.Loading ?: return null
        if (state.shopId != shopId) return null
        if ((state.shop == null) != expectsRequestedShop) return null
        return state
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

    private fun bookmarkTaskKey(shopId: String): String = "map-bookmark:$shopId"

    private fun shopNotificationTaskKey(shopId: String): String = "map-shop-notification:$shopId"

    private fun hiddenShopTaskKey(shopId: String): String = "map-hidden-shop:$shopId"

    companion object {
        private val SEARCH_QUERY_LENGTH = 2..15
        private const val SEARCH_RESULT_LIMIT = 50
        private const val SEARCH_TASK_KEY = "map-search"
        private const val SHOP_DETAIL_TASK_KEY = "map-shop-detail"
        private const val SHOP_REPORT_TASK_KEY = "map-shop-report"
        private const val SIGN_IN_TASK_KEY = "map-sign-in"
        private const val PERSONALIZED_SHOPS_TASK_KEY = "map-personalized-shops"
        private const val OPEN_FILTER_REFRESH_INTERVAL_MILLIS = 60_000L
    }
}
