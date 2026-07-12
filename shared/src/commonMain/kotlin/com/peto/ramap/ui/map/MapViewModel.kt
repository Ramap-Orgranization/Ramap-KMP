package com.peto.ramap.ui.map

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.base.BaseViewModel
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.retryOnce
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.Personalization
import com.peto.ramap.domain.model.PlaceReportTextParser
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShopFilter
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.SearchQuery
import com.peto.ramap.domain.model.ShopInformationField
import com.peto.ramap.domain.model.ShopInformationReport
import com.peto.ramap.domain.model.UnregisteredPlaceReport
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.network.NaverReverseGeocoder
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.map.contract.MapIntent
import com.peto.ramap.ui.map.contract.MapSideEffect
import com.peto.ramap.ui.map.contract.MapUiState
import com.peto.ramap.ui.map.contract.OnAccountDeleteConfirmed
import com.peto.ramap.ui.map.contract.OnBookmarkToggled
import com.peto.ramap.ui.map.contract.OnBoundsChanged
import com.peto.ramap.ui.map.contract.OnCategoryFilterToggled
import com.peto.ramap.ui.map.contract.OnCurrentLocationReportSubmitted
import com.peto.ramap.ui.map.contract.OnFilterCleared
import com.peto.ramap.ui.map.contract.OnHiddenToggled
import com.peto.ramap.ui.map.contract.OnInitialMapRetryClicked
import com.peto.ramap.ui.map.contract.OnKakaoLoginClicked
import com.peto.ramap.ui.map.contract.OnLocationPermissionBlocked
import com.peto.ramap.ui.map.contract.OnLogoutClicked
import com.peto.ramap.ui.map.contract.OnMyLocationChanged
import com.peto.ramap.ui.map.contract.OnPersonalizationViewChanged
import com.peto.ramap.ui.map.contract.OnQueryChanged
import com.peto.ramap.ui.map.contract.OnSearchResultsDismissed
import com.peto.ramap.ui.map.contract.OnShopDetailDismissed
import com.peto.ramap.ui.map.contract.OnShopDetailRetryClicked
import com.peto.ramap.ui.map.contract.OnShopReportSubmitted
import com.peto.ramap.ui.map.contract.OnShopSelected
import com.peto.ramap.ui.map.contract.OnUnregisteredPlaceReportSubmitted
import com.peto.ramap.ui.map.contract.ShowLoginGuide
import com.peto.ramap.ui.map.contract.ShowToast
import com.peto.ramap.ui.map.model.InitialMapLoadState
import com.peto.ramap.ui.map.model.MapPersonalization
import com.peto.ramap.ui.map.model.SearchUiState
import com.peto.ramap.ui.map.model.ShopDetail
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.account_delete_failure_message
import ramap.shared.generated.resources.account_delete_success_message
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.filter_empty_visible_result_message
import ramap.shared.generated.resources.hidden_shop_search_result_message
import ramap.shared.generated.resources.kakao_login_failure_message
import ramap.shared.generated.resources.location_permission_enable_message
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.personalization_update_failure_message
import ramap.shared.generated.resources.place_report_existing_shop_message
import ramap.shared.generated.resources.place_report_failure_message
import ramap.shared.generated.resources.place_report_invalid_url_message
import ramap.shared.generated.resources.place_report_location_unavailable_message
import ramap.shared.generated.resources.place_report_success_message
import ramap.shared.generated.resources.shop_information_report_failure_message
import ramap.shared.generated.resources.shop_information_report_success_message
import kotlin.time.Duration.Companion.milliseconds

class MapViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val shopWaitingSystemRepository: ShopWaitingSystemRepository,
    private val personalizationRepository: PersonalizationRepository,
    private val reportRepository: ShopReportRepository,
    private val loginRepository: LoginRepository,
    private val reverseGeocoder: NaverReverseGeocoder? = null,
) : BaseViewModel<MapUiState, MapIntent, MapSideEffect>(initialState = MapUiState()) {
    private var boundsLoadJob: Job? = null
    private var boundsLoadRequestId = 0L
    private var lastLoadedBounds: MapBounds? = null
    private var searchJob: Job? = null
    private var searchRequestId = 0L
    private var detailJob: Job? = null
    private var shopReportJob: Job? = null
    private var placeReportJob: Job? = null

    init {
        viewModelScope.launch { observeSessionStatus() }
    }

    override suspend fun handleIntent(intent: MapIntent) {
        when (intent) {
            is OnBoundsChanged -> scheduleRamenShopsLoad(intent.bounds)
            is OnMyLocationChanged -> updateMyLocation(intent.location)
            is OnShopSelected -> selectShop(intent.shop)
            is OnShopDetailDismissed -> dismissShopDetail()
            is OnSearchResultsDismissed -> dismissSearchResults()
            OnInitialMapRetryClicked -> retryInitialMapLoad()
            OnShopDetailRetryClicked -> retryShopDetailLoad()
            is OnQueryChanged -> updateQuery(intent.query)
            is OnCategoryFilterToggled -> toggleCategoryFilter(intent.category)
            is OnFilterCleared -> clearFilter()
            is OnBookmarkToggled -> toggleBookmark(intent.shop)
            is OnHiddenToggled -> toggleHidden(intent.shop)
            is OnShopReportSubmitted ->
                submitShopInformationReport(
                    wrongFields = intent.wrongFields,
                    description = intent.description,
                )

            is OnUnregisteredPlaceReportSubmitted ->
                submitUnregisteredPlaceReport(intent.placeUrl)

            OnCurrentLocationReportSubmitted -> submitCurrentLocationReport()

            is OnPersonalizationViewChanged -> changePersonalizationView(intent.view)
            OnKakaoLoginClicked -> signInWithKakao()
            OnLogoutClicked -> signOut()
            OnAccountDeleteConfirmed -> deleteAccount()
            OnLocationPermissionBlocked -> showLocationPermissionBlockedToast()
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
                isDeletingAccount = if (isAuthenticated) isDeletingAccount else false,
                bookmarkedShopIds = if (isAuthenticated) bookmarkedShopIds else emptySet(),
                hiddenShopIds = if (isAuthenticated) hiddenShopIds else emptySet(),
                personalizationView = if (isAuthenticated) personalizationView else MapPersonalization.ALL,
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
        loadShopDetail(shop.id)
    }

    private fun dismissShopDetail() {
        detailJob?.cancel()
        reduce { copy(selectedShop = null, shopDetailState = LoadState.Idle) }
    }

    private suspend fun updateMyLocation(location: Location) {
        reduce {
            copy(
                currentLocation = location,
                currentAddress = null,
            )
        }
        requestAddress(location)
    }

    private suspend fun requestAddress(location: Location) {
        val geocoder = reverseGeocoder ?: return
        handleResult(
            result = geocoder.address(location),
            onSuccess = ::updateCurrentAddress,
        )
    }

    private fun updateCurrentAddress(address: String?) {
        reduce { copy(currentAddress = address) }
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
            handleSingleSearchResult(currentState.searchResultShops.singleOrNull())
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
        handleResult(
            result = personalizationRepository.fetchPersonalization(),
            onSuccess = ::updatePersonalization,
            onError = { showPersonalizationUpdateFailure() },
        )
    }

    private suspend fun updatePersonalization(personalization: Personalization) {
        val hiddenShopIds = personalization.hiddenShopIds
        val bookmarkedShopIds = personalization.bookmarkedShopIds - hiddenShopIds

        loadPersonalizedShops(bookmarkedShopIds + hiddenShopIds)
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

        handleResult(
            result = ramenShopRepository.fetchRamenShopsByIds(shopIds),
            onSuccess = ::mergePersonalizedShops,
        )
    }

    private fun mergePersonalizedShops(result: RamenShops) {
        reduce { copy(shops = RamenShops(shops + result)) }
    }

    private suspend fun toggleBookmark(shop: RamenShop) {
        if (!currentState.isLoggedIn) {
            trySideEffect(ShowLoginGuide)
            return
        }

        val isBookmarked = shop.id in currentState.bookmarkedShopIds
        reduceBookmarkState(shop.id)
        postBookmark(shop.id, isBookmarked)
    }

    private suspend fun postBookmark(
        id: String,
        isBookmarked: Boolean,
    ) {
        handleResult(
            result = updateBookmarkPersonalization(id, isBookmarked),
            onError = { handleBookmarkFailure(id) },
        )
    }

    private fun handleBookmarkFailure(shopId: String) {
        reduceBookmarkState(shopId)
        showPersonalizationUpdateFailure()
    }

    private fun reduceBookmarkState(shopId: String) {
        reduce {
            copy(
                bookmarkedShopIds =
                    if (shopId in bookmarkedShopIds) bookmarkedShopIds - shopId else bookmarkedShopIds + shopId,
            )
        }
    }

    private suspend fun updateBookmarkPersonalization(
        shopId: String,
        isBookmarked: Boolean,
    ): RamapResult<Unit> {
        if (isBookmarked) {
            return personalizationRepository.removeBookmark(shopId)
        }

        return when (val result = personalizationRepository.addBookmark(shopId)) {
            is RamapResult.Error -> result
            is RamapResult.Success -> {
                loadPersonalizedShops(setOf(shopId))
                result
            }
        }
    }

    private suspend fun toggleHidden(shop: RamenShop) {
        if (!currentState.isLoggedIn) {
            trySideEffect(ShowLoginGuide)
            return
        }

        if (shop.id in currentState.hiddenShopIds) {
            unhideShop(shop)
        } else {
            hideShop(shop)
        }
    }

    private suspend fun hideShop(shop: RamenShop) {
        val shouldRemoveBookmark = shop.id in currentState.bookmarkedShopIds
        val previousBookmarkedShopIds = currentState.bookmarkedShopIds
        val previousHiddenShopIds = currentState.hiddenShopIds
        val previousSelectedShop = currentState.selectedShop
        val previousSearch = currentState.search

        reduceHideShopState(shop.id, shouldRemoveBookmark)
        handleResult(
            result = persistHiddenShop(shop.id, shouldRemoveBookmark),
            onError = {
                restoreHiddenShopState(
                    previousBookmarkedShopIds,
                    previousHiddenShopIds,
                    previousSelectedShop,
                    previousSearch,
                )
                showPersonalizationUpdateFailure()
            },
        )
    }

    private suspend fun persistHiddenShop(
        shopId: String,
        shouldRemoveBookmark: Boolean,
    ): RamapResult<Unit> = personalizationRepository.hideShop(shopId, removeBookmark = shouldRemoveBookmark)

    private suspend fun unhideShop(shop: RamenShop) {
        reduceUnhideShopState(shop.id)
        handleResult(
            result = personalizationRepository.unhideShop(shop.id),
            onError = { handleUnhideShopFailure(shop.id) },
        )
    }

    private fun handleUnhideShopFailure(shopId: String) {
        reduceHideShopState(shopId, shouldRemoveBookmark = false)
        showPersonalizationUpdateFailure()
    }

    private fun restoreHiddenShopState(
        bookmarkedShopIds: Set<String>,
        hiddenShopIds: Set<String>,
        selectedShop: RamenShop?,
        search: SearchUiState,
    ) {
        reduce {
            copy(
                bookmarkedShopIds = bookmarkedShopIds,
                hiddenShopIds = hiddenShopIds,
                selectedShop = selectedShop,
                search = search,
            )
        }
    }

    private fun showPersonalizationUpdateFailure() {
        showToast(Res.string.personalization_update_failure_message, ToastType.ERROR)
    }

    private fun reduceHideShopState(
        shopId: String,
        shouldRemoveBookmark: Boolean,
    ) {
        reduce {
            val shouldCloseSelectedShop = selectedShop?.id == shopId

            copy(
                hiddenShopIds = hiddenShopIds + shopId,
                bookmarkedShopIds = if (shouldRemoveBookmark) bookmarkedShopIds - shopId else bookmarkedShopIds,
                selectedShop = selectedShop?.takeUnless { shouldCloseSelectedShop },
                search =
                    updateSearchState(
                        search = search,
                        shouldCloseSelectedShop = shouldCloseSelectedShop,
                    ),
            )
        }
    }

    private fun reduceUnhideShopState(shopId: String) {
        reduce {
            copy(
                hiddenShopIds = hiddenShopIds - shopId,
                selectedShop =
                    if (selectedShop?.id == shopId) {
                        selectedShop.copy(isVisible = true)
                    } else {
                        selectedShop
                    },
            )
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
            trySideEffect(ShowLoginGuide)
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

    private fun submitShopInformationReport(
        wrongFields: Set<ShopInformationField>,
        description: String,
    ) {
        val shop = currentState.selectedShop ?: return
        if (wrongFields.isEmpty() && description.isBlank()) return
        if (shopReportJob?.isActive == true) return

        shopReportJob =
            viewModelScope.launch {
                handleResult(
                    result =
                        reportRepository.submit(
                            ShopInformationReport(
                                shopId = shop.id,
                                shopName = shop.name,
                                wrongFields = wrongFields,
                                description = description.trim(),
                            ),
                        ),
                    onSuccess = { showShopInformationReportSuccess() },
                    onError = { showShopInformationReportFailure() },
                )
            }
    }

    private fun showShopInformationReportSuccess() {
        showToast(Res.string.shop_information_report_success_message)
    }

    private fun showShopInformationReportFailure() {
        showToast(Res.string.shop_information_report_failure_message, ToastType.ERROR)
    }

    private fun submitUnregisteredPlaceReport(placeUrl: String) {
        startPlaceReport {
            val extractedPlaceUrl = PlaceReportTextParser.extractSupportedUrl(placeUrl)
            if (extractedPlaceUrl == null) {
                showToast(Res.string.place_report_invalid_url_message, ToastType.ERROR)
                return@startPlaceReport
            }

            val loadedShop =
                currentState.shops.values.firstOrNull {
                    PlaceReportTextParser.matchesSharedPlace(placeUrl, it)
                }
            val existingShop = loadedShop ?: findExistingShop(placeUrl)

            if (existingShop != null) {
                showToast(Res.string.place_report_existing_shop_message)
            } else {
                submitUnregisteredPlaceReport(UnregisteredPlaceReport(placeUrl = extractedPlaceUrl))
            }
        }
    }

    private suspend fun findExistingShop(placeUrl: String): RamenShop? {
        val placeName = PlaceReportTextParser.extractSharedPlaceName(placeUrl) ?: return null
        return when (val result = ramenShopRepository.searchRamenShops(SearchQuery(placeName), SEARCH_RESULT_LIMIT)) {
            is RamapResult.Success ->
                result.data.values.firstOrNull {
                    PlaceReportTextParser.matchesSharedPlace(placeUrl, it)
                }
            is RamapResult.Error -> null
        }
    }

    private fun submitCurrentLocationReport() {
        startPlaceReport {
            val location = currentState.currentLocation
            if (location == null) {
                showToast(Res.string.place_report_location_unavailable_message, ToastType.ERROR)
                return@startPlaceReport
            }

            submitUnregisteredPlaceReport(UnregisteredPlaceReport(location = location))
        }
    }

    private fun startPlaceReport(block: suspend () -> Unit) {
        if (placeReportJob?.isActive == true) return
        placeReportJob =
            viewModelScope.launch {
                block()
            }
    }

    private suspend fun submitUnregisteredPlaceReport(report: UnregisteredPlaceReport) {
        handleResult(
            result = reportRepository.submit(report),
            onSuccess = { showPlaceReportSuccess() },
            onError = { showPlaceReportFailure() },
        )
    }

    private fun showPlaceReportSuccess() {
        showToast(Res.string.place_report_success_message)
    }

    private fun showPlaceReportFailure() {
        showToast(Res.string.place_report_failure_message, ToastType.ERROR)
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

    private suspend fun signOut() {
        handleResult(result = loginRepository.signOut())
    }

    private suspend fun deleteAccount() {
        if (currentState.isDeletingAccount) return
        reduce { copy(isDeletingAccount = true) }
        handleResult(
            result = loginRepository.deleteAccount(),
            onSuccess = { showAccountDeleteSuccess() },
            onError = { handleAccountDeleteFailure() },
        )
    }

    private fun showAccountDeleteSuccess() {
        showToast(Res.string.account_delete_success_message, ToastType.SUCCESS)
    }

    private fun handleAccountDeleteFailure() {
        reduce { copy(isDeletingAccount = false) }
        showToast(Res.string.account_delete_failure_message, ToastType.ERROR)
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
            viewModelScope.launch {
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
        val result =
            ramenShopRepository.searchRamenShops(
                query = query,
                limit = SEARCH_RESULT_LIMIT,
            )
        if (requestId != searchRequestId) return

        handleResult(
            result = result,
            onSuccess = { shops -> handleSearchSuccess(query, shops) },
            onError = { showDataLoadFailure() },
        )
    }

    private fun handleSearchSuccess(
        query: SearchQuery,
        shops: RamenShops,
    ) {
        reduceSearchResult(query, shops)
        handleSingleSearchResult(currentState.searchResultShops.singleOrNull())
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
        detailJob?.cancel()
        detailJob =
            viewModelScope.launch {
                reduce { copy(shopDetailState = LoadState.Loading) }
                handleResult(
                    result = retryOnce { fetchShopDetail(shopId) },
                    onSuccess = { detail -> handleShopDetailSuccess(shopId, detail) },
                    onError = { handleShopDetailFailure(shopId) },
                )
            }
    }

    private fun handleShopDetailSuccess(
        shopId: String,
        detail: ShopDetail,
    ) {
        if (currentState.selectedShop?.id != shopId) return
        reduce {
            copy(
                selectedShop = detail.shop,
                shopWaiting = shopWaiting + (shopId to detail.waitingSystem),
                shopDetailState = LoadState.Content(detail),
            )
        }
    }

    private fun handleShopDetailFailure(shopId: String) {
        if (currentState.selectedShop?.id == shopId) {
            reduce { copy(shopDetailState = LoadState.Error) }
        }
    }

    private suspend fun fetchShopDetail(shopId: String): RamapResult<ShopDetail> =
        coroutineScope {
            val shopResult = async { ramenShopRepository.fetchRamenShopsByIds(setOf(shopId)) }
            val waitingResult = async { shopWaitingSystemRepository.fetchShopWaitingSystem(shopId) }

            when (val shops = shopResult.await()) {
                is RamapResult.Error -> shops
                is RamapResult.Success -> {
                    val shop =
                        shops.data[shopId]
                            ?: return@coroutineScope RamapResult.Error(
                                RamapError.Unknown(IllegalStateException("매장 상세를 찾을 수 없습니다: $shopId")),
                            )
                    when (val waiting = waitingResult.await()) {
                        is RamapResult.Error -> waiting
                        is RamapResult.Success ->
                            RamapResult.Success(
                                ShopDetail(
                                    shop,
                                    waiting.data,
                                ),
                            )
                    }
                }
            }
        }

    private fun retryShopDetailLoad() {
        currentState.selectedShop?.id?.let(::loadShopDetail)
    }

    /**
     * 지도 이동 이벤트를 바로 API 호출로 연결하지 않고 짧게 지연한다.
     *
     * 사용자가 지도를 연속해서 움직이면 이전 작업을 취소하고 마지막 bounds만 남겨,
     * 드래그 중간 지점마다 라멘 가게 목록을 다시 조회하지 않도록 한다.
     */
    private fun scheduleRamenShopsLoad(bounds: MapBounds) {
        reduce {
            copy(bounds = bounds)
        }

        boundsLoadJob?.cancel()
        val requestId = ++boundsLoadRequestId
        boundsLoadJob =
            viewModelScope.launch {
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

        val isInitialLoad = currentState.initialMapLoadState != InitialMapLoadState.CONTENT
        val result =
            if (isInitialLoad) {
                retryOnce { ramenShopRepository.fetchRamenShops(bounds) }
            } else {
                ramenShopRepository.fetchRamenShops(bounds)
            }
        if (requestId != boundsLoadRequestId) return

        handleResult(
            result = result,
            onSuccess = { shops -> handleRamenShopsLoadSuccess(bounds, shops) },
            onError = { handleRamenShopsLoadFailure(isInitialLoad) },
        )
    }

    private fun handleRamenShopsLoadSuccess(
        bounds: MapBounds,
        shops: RamenShops,
    ) {
        lastLoadedBounds = bounds
        val mergedShops = mergeShops(shops)
        reduce {
            if (this.shops == mergedShops && initialMapLoadState == InitialMapLoadState.CONTENT) {
                this
            } else {
                copy(shops = mergedShops, initialMapLoadState = InitialMapLoadState.CONTENT)
            }
        }
    }

    private fun handleRamenShopsLoadFailure(isInitialLoad: Boolean) {
        if (isInitialLoad) {
            reduce { copy(initialMapLoadState = InitialMapLoadState.ERROR) }
        } else {
            showDataLoadFailure()
        }
    }

    private fun retryInitialMapLoad() {
        lastLoadedBounds = null
        reduce { copy(initialMapLoadState = InitialMapLoadState.LOADING) }
        scheduleRamenShopsLoad(currentState.bounds)
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
