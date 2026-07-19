package com.peto.ramap.ui.main.map

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.place.PlaceSearchResults
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
import com.peto.ramap.domain.repository.PlaceSearchRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.location.CurrentLocationStore
import com.peto.ramap.ui.main.map.config.DefaultMapConfig
import com.peto.ramap.ui.main.map.contract.MapIntent
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkedShopsToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBoundsChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCameraPositionChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCategoryFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnFilterCleared
import com.peto.ramap.ui.main.map.contract.MapIntent.OnHiddenToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnInitialLocationFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnKakaoLoginClicked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLocationPermissionBlocked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMyLocationChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnPlaceSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnQueryChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSearchResultsDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopIdSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopNotificationToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopReportSubmitted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnViewportLoadRetry
import com.peto.ramap.ui.main.map.contract.MapSideEffect
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowLoginGuide
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowToast
import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.main.map.model.CameraPosition
import com.peto.ramap.ui.main.map.model.LocationFocusStatus
import com.peto.ramap.ui.main.map.search.MapSearchController
import com.peto.ramap.ui.main.map.search.MapSearchResult
import com.peto.ramap.ui.main.map.viewport.ViewportLoadResult
import com.peto.ramap.ui.main.map.viewport.ViewportShopLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.filter_empty_visible_result_message
import ramap.shared.generated.resources.hidden_shop_notification_unavailable_message
import ramap.shared.generated.resources.hidden_shop_search_result_message
import ramap.shared.generated.resources.hide_shop_success_message
import ramap.shared.generated.resources.kakao_login_failure_message
import ramap.shared.generated.resources.location_permission_enable_message
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.personalization_update_failure_message
import ramap.shared.generated.resources.search_result_empty_message
import ramap.shared.generated.resources.shop_information_report_failure_message
import ramap.shared.generated.resources.shop_information_report_success_message

class MapViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val loginRepository: LoginRepository,
    private val currentLocationStore: CurrentLocationStore,
    private val placeSearchRepository: PlaceSearchRepository,
    private val shopReportRepository: ShopReportRepository,
    private val personalizationStore: ShopPersonalizationStore,
    private val fetchShopDetailUseCase: FetchShopDetailUseCase,
) : BaseViewModel<MapUiState, MapIntent, MapSideEffect>(initialState = MapUiState()) {
    private val searchController =
        MapSearchController(
            ramenShopRepository = ramenShopRepository,
            placeSearchRepository = placeSearchRepository,
            coroutineScope = viewModelScope,
        )
    private val viewportShopLoader = ViewportShopLoader(ramenShopRepository, viewModelScope)
    private var shopDetailJob: Job? = null

    init {
        viewModelScope.launch { observeSessionState() }
        viewModelScope.launch { observePersonalization() }
    }

    private suspend fun observePersonalization() {
        personalizationStore.state.collectLatest { personalization ->
            if (!currentState.isLoggedIn) return@collectLatest
            updatePersonalization(personalization)
        }
    }

    override suspend fun handleIntent(intent: MapIntent) {
        when (intent) {
            is OnBoundsChanged -> scheduleRamenShopsLoad(intent.bounds)
            OnViewportLoadRetry -> scheduleRamenShopsLoad(currentState.bounds)
            is OnCameraPositionChanged -> updateCameraPosition(intent.position)
            is OnMyLocationChanged -> updateMyLocation(intent.location)
            OnInitialLocationFocusConsumed -> consumeInitialLocationFocus()
            is OnShopSelected -> selectShop(intent.shop, intent.shouldFocus)
            is OnShopIdSelected -> selectShop(intent.shopId)
            is OnShopDetailDismissed -> dismissShopDetail()
            is OnSearchResultsDismissed -> dismissSearchResults()
            is OnQueryChanged -> updateQuery(intent.query)
            is OnPlaceSelected -> selectPlace(intent.place)
            is OnCategoryFilterToggled -> toggleCategoryFilter(intent.category)
            OnBookmarkedShopsToggled -> toggleBookmarkedView()
            is OnFilterCleared -> clearFilter()
            is OnBookmarkToggled -> toggleBookmark(intent.shop)
            is OnShopNotificationToggled -> toggleShopNotification(intent.shop)
            is OnHiddenToggled -> toggleHidden(intent.shop)
            is OnShopReportSubmitted ->
                submitShopInformationReport(
                    wrongFields = intent.wrongFields,
                    description = intent.description,
                )

            OnKakaoLoginClicked -> signInWithKakao()
            OnLocationPermissionBlocked -> showLocationPermissionBlockedToast()
        }
    }

    private suspend fun observeSessionState() {
        loginRepository.sessionState.collectLatest { sessionState ->
            val isAuthenticated = sessionState == LoginSessionState.AUTHENTICATED
            updateAuthState(isAuthenticated)
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
        val isCurrentSearchResult = shop.id in currentState.search.results
        val shopId = shop.id
        val cachedDetail = fetchShopDetailUseCase.findCached(shopId)
        reduce {
            copy(
                selectedShop = cachedDetail?.shop?.copy(isVisible = shop.isVisible) ?: shop,
                shouldFocusSelectedShop = shouldFocus,
                shopDetail = cachedDetail,
                shopWaiting =
                    cachedDetail?.let { shopWaiting + (shop.id to it.waitingSystem) }
                        ?: shopWaiting,
                isShopDetailLoading = cachedDetail == null,
                search = search.consumeResultFocus(isCurrentSearchResult),
            )
        }
        if (cachedDetail != null) return

        loadShopDetail(shopId)
    }

    private fun dismissShopDetail() {
        cancelShopDetailLoad()
        reduce {
            copy(
                selectedShop = null,
                shopDetail = null,
                isShopDetailLoading = false,
            )
        }
    }

    private suspend fun selectShop(shopId: String) {
        if (shopId.isBlank()) return
        fetchShopDetailUseCase.findCached(shopId)?.let { detail ->
            selectShop(detail.shop)
            return
        }

        handleResult(
            result = ramenShopRepository.fetchRamenShops(setOf(shopId)),
            onSuccess = { shops -> shops[shopId]?.let { selectShop(it) } },
            onError = {},
        )
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

    private fun updateCameraPosition(position: CameraPosition) {
        reduce {
            copy(
                cameraPosition = position,
                shouldFocusSelectedShop = false,
            )
        }
    }

    private fun dismissSearchResults() {
        reduce { copy(search = search.dismissResults()) }
    }

    private fun selectPlace(place: PlaceSearchResult) {
        reduce {
            copy(
                search = search.selectPlace(place),
                selectedShop = null,
            )
        }
    }

    private suspend fun updateQuery(query: String) {
        val normalizedQuery = SearchQuery(query).normalizeShopSearchQuery()
        val hasCurrentSearchResults = currentState.search.hasLoadedResultsFor(normalizedQuery)

        cancelShopDetailLoad()
        reduce {
            copy(
                search = search.updateInput(query),
                selectedShop = null,
            )
        }

        if (
            normalizedQuery.value.isNotBlank() &&
            hasCurrentSearchResults &&
            currentState.search.results.isNotEmpty()
        ) {
            searchController.cancel()
            handleSingleSearchResult(currentState.searchResultShops.singleShopOrNull())
            return
        }

        val searchCenter =
            currentState.cameraPosition?.center
                ?: Location(DefaultMapConfig.LATITUDE, DefaultMapConfig.LONGITUDE)
        searchController.search(normalizedQuery, searchCenter, ::handleSearchResult)
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

    private suspend fun updatePersonalization(personalization: ShopPersonalization) {
        val hiddenShopIds = personalization.hiddenShopIds
        val bookmarkedShopIds = personalization.bookmarkedShopIds

        loadPersonalizedShops(bookmarkedShopIds + hiddenShopIds)
        reduce {
            val selectedShopWasHidden = selectedShop?.id in this.hiddenShopIds
            val selectedShopIsHidden = selectedShop?.id in hiddenShopIds
            val selectedShopBecameHidden = !selectedShopWasHidden && selectedShopIsHidden
            val selectedShopBecameVisible = selectedShopWasHidden && !selectedShopIsHidden
            copy(
                bookmarkedShopIds = bookmarkedShopIds,
                hiddenShopIds = hiddenShopIds,
                notificationShopIds = personalization.notificationShopIds,
                search = if (selectedShopBecameHidden) search.dismissResults() else search,
                selectedShop =
                    when {
                        selectedShopBecameHidden -> null
                        selectedShopBecameVisible -> selectedShop?.copy(isVisible = true)
                        else -> selectedShop
                    },
            )
        }
    }

    private suspend fun loadPersonalizedShops(shopIds: Set<String>) {
        if (shopIds.isEmpty()) return

        handleResult(
            result = ramenShopRepository.fetchRamenShops(shopIds),
            onSuccess = ::mergePersonalizedShops,
        )
    }

    private fun mergePersonalizedShops(result: RamenShops) {
        reduce { copy(shops = RamenShops(shops + result)) }
    }

    private suspend fun toggleBookmark(shop: RamenShop) {
        if (!isLoggedInOrShowGuide()) return

        val isBookmarked = shop.id in currentState.bookmarkedShopIds
        postBookmark(shop.id, isBookmarked)
    }

    private suspend fun toggleShopNotification(shop: RamenShop) {
        if (!isLoggedInOrShowGuide()) return
        val isHiddenShop = shop.id in currentState.hiddenShopIds
        if (isHiddenShop) {
            showToast(Res.string.hidden_shop_notification_unavailable_message)
            return
        }

        val wasEnabled = shop.id in currentState.notificationShopIds
        postShopNotification(shop.id, wasEnabled)
    }

    private suspend fun postShopNotification(
        shopId: String,
        wasEnabled: Boolean,
    ) {
        val enabled = !wasEnabled
        handleResult(
            result = personalizationStore.updateShopNotification(shopId, enabled),
            onError = { showPersonalizationUpdateFailure() },
        )
    }

    private suspend fun postBookmark(
        shopId: String,
        isBookmarked: Boolean,
    ) {
        val enabled = !isBookmarked
        handleResult(
            result = personalizationStore.updateBookmark(shopId, enabled),
            onSuccess = {
                if (!isBookmarked) loadPersonalizedShops(setOf(shopId))
            },
            onError = { showPersonalizationUpdateFailure() },
        )
    }

    private suspend fun toggleHidden(shop: RamenShop) {
        if (!isLoggedInOrShowGuide()) return

        if (shop.id in currentState.hiddenShopIds) {
            unhideShop(shop)
        } else {
            hideShop(shop)
        }
    }

    private suspend fun hideShop(shop: RamenShop) {
        handleResult(
            result = personalizationStore.hideShop(shop.id),
            onSuccess = { showHiddenShopSuccess() },
            onError = { showPersonalizationUpdateFailure() },
        )
    }

    private fun showHiddenShopSuccess() {
        showToast(Res.string.hide_shop_success_message)
    }

    private suspend fun unhideShop(shop: RamenShop) {
        handleResult(
            result = personalizationStore.unhideShop(shop.id),
            onError = { showPersonalizationUpdateFailure() },
        )
    }

    private fun showPersonalizationUpdateFailure() {
        showToast(Res.string.personalization_update_failure_message, ToastType.ERROR)
    }

    private fun toggleBookmarkedView() {
        if (!isLoggedInOrShowGuide()) return

        reduce {
            val willShowBookmarkedShops = !isBookmarkedView
            copy(
                isBookmarkedView = willShowBookmarkedShops,
                selectedShop =
                    selectedShop?.takeIf { shop ->
                        if (willShowBookmarkedShops) {
                            shop.id in bookmarkedShopIds
                        } else {
                            shop.id !in hiddenShopIds
                        }
                    },
                search = search.showResults(),
            )
        }
    }

    private fun isLoggedInOrShowGuide(): Boolean {
        if (currentState.isLoggedIn) return true

        trySideEffect(ShowLoginGuide)
        return false
    }

    private suspend fun submitShopInformationReport(
        wrongFields: Set<ShopInformationField>,
        description: String,
    ) {
        val shop = currentState.selectedShop ?: return
        if (wrongFields.isEmpty() && description.isBlank()) return

        val report =
            ShopInformationReport(
                shopId = shop.id,
                shopName = shop.name,
                wrongFields = wrongFields,
                description = description.trim(),
            )
        handleResult(
            result = shopReportRepository.submitShopInformationReport(report),
            onSuccess = { showShopInformationReportSuccess() },
            onError = { showShopInformationReportFailure() },
        )
    }

    private fun showShopInformationReportSuccess() {
        showToast(Res.string.shop_information_report_success_message)
    }

    private fun showShopInformationReportFailure() {
        showToast(Res.string.shop_information_report_failure_message, ToastType.ERROR)
    }

    private suspend fun signInWithKakao() {
        handleResult(
            result = loginRepository.signInWithKakao(),
            onError = { showKakaoLoginFailure() },
        )
    }

    private fun showKakaoLoginFailure() {
        showToast(Res.string.kakao_login_failure_message, ToastType.ERROR)
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

    private fun clearSearchResults() {
        reduce {
            copy(
                search = search.clearResults(),
            )
        }
    }

    private suspend fun handleSearchResult(result: MapSearchResult) {
        when (result) {
            MapSearchResult.Cleared -> clearSearchResults()
            is MapSearchResult.Loaded -> {
                reduceSearchResult(result.query, result.shops)
                if (result.shops.isNotEmpty()) {
                    handleSingleSearchResult(currentState.searchResultShops.singleShopOrNull())
                    return
                }

                handlePlaceSearchSuccess(result.places)
            }

            is MapSearchResult.Failed -> {
                handleError(result.error)
                showDataLoadFailure()
            }
        }
    }

    private fun handlePlaceSearchSuccess(results: PlaceSearchResults) {
        reduce { copy(search = search.updatePlaceResults(results)) }
        when (results.size) {
            0 -> showToast(Res.string.search_result_empty_message)
            1 -> selectPlace(results.single())
        }
    }

    private fun showDataLoadFailure() {
        showToast(Res.string.data_load_failure_message, ToastType.ERROR)
    }

    private suspend fun handleSingleSearchResult(shop: RamenShop?) {
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
        trySideEffect(
            ShowToast(
                ToastData(
                    message = messageResource,
                    type = type,
                ),
            ),
        )
    }

    private fun showLocationPermissionBlockedToast() {
        trySideEffect(
            ShowToast(
                ToastData(
                    message = Res.string.location_permission_enable_message,
                    type = ToastType.DEFAULT,
                    action =
                        ToastAction(
                            label = Res.string.location_permission_settings_action,
                        ),
                ),
            ),
        )
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

    private fun loadShopDetail(shopId: String) {
        shopDetailJob?.cancel()
        shopDetailJob =
            viewModelScope.launch {
                when (val result = fetchShopDetailUseCase(shopId)) {
                    is RamapResult.Success -> {
                        if (currentState.selectedShop?.id != shopId) return@launch
                        handleShopDetailSuccess(result.data)
                    }

                    is RamapResult.Error -> {
                        if (currentState.selectedShop?.id != shopId) return@launch
                        handleShopDetailFailure(result.error)
                    }
                }
            }
    }

    private fun cancelShopDetailLoad() {
        shopDetailJob?.cancel()
        shopDetailJob = null
    }

    private fun handleShopDetailSuccess(detail: ShopDetail) {
        val selectedShop = currentState.selectedShop ?: return

        reduce {
            copy(
                selectedShop = detail.shop.copy(isVisible = selectedShop.isVisible),
                shopWaiting = shopWaiting + (selectedShop.id to detail.waitingSystem),
                shopDetail = detail,
                isShopDetailLoading = false,
            )
        }
    }

    private fun handleShopDetailFailure(error: RamapError) {
        handleError(error)
        reduce { copy(shopDetail = null, isShopDetailLoading = false) }
        showDataLoadFailure()
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
            is ViewportLoadResult.Loaded -> {
                val mergedShops = mergeShops(result.shops)
                reduce {
                    if (shops == mergedShops && !hasViewportLoadFailed) {
                        this
                    } else {
                        copy(
                            shops = mergedShops,
                            hasViewportLoadFailed = false,
                        )
                    }
                }
            }

            is ViewportLoadResult.Failed -> {
                handleError(result.error)
                reduce { copy(hasViewportLoadFailed = true) }
            }
        }
    }

    private fun mergeShops(newShops: RamenShops): RamenShops =
        RamenShops(
            currentState.shops + newShops,
        )
}
