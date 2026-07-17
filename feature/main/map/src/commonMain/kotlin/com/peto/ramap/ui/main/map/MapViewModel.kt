package com.peto.ramap.ui.main.map

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.retryOnce
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.report.PlaceReportTextParser
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.report.ShopInformationReport
import com.peto.ramap.domain.model.report.UnregisteredPlaceReport
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShopFilter
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.CurrentLocationStore
import com.peto.ramap.ui.main.map.contract.MapIntent
import com.peto.ramap.ui.main.map.contract.MapIntent.OnAccountDeleteConfirmed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkedShopsToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBoundsChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCategoryFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCurrentLocationReportSubmitted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnFilterCleared
import com.peto.ramap.ui.main.map.contract.MapIntent.OnHiddenToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnInitialLocationFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnInitialMapRetryClicked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnKakaoLoginClicked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLocationPermissionBlocked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLogoutClicked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMyLocationChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnPersonalizationViewChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnQueryChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSearchResultsDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopIdSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopNotificationToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopReportSubmitted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnUnregisteredPlaceReportSubmitted
import com.peto.ramap.ui.main.map.contract.MapSideEffect
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowLoginGuide
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowToast
import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.main.map.model.InitialMapLoadState
import com.peto.ramap.ui.main.map.model.MapPersonalization
import com.peto.ramap.ui.main.map.model.SearchUiState
import com.peto.ramap.ui.main.map.model.ShopDetail
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
import ramap.shared.generated.resources.hidden_shop_notification_unavailable_message
import ramap.shared.generated.resources.hidden_shop_search_result_message
import ramap.shared.generated.resources.hide_shop_success_message
import ramap.shared.generated.resources.kakao_login_failure_message
import ramap.shared.generated.resources.location_permission_enable_message
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.personalization_update_failure_message
import ramap.shared.generated.resources.place_report_existing_shop_message
import ramap.shared.generated.resources.place_report_failure_message
import ramap.shared.generated.resources.place_report_invalid_url_message
import ramap.shared.generated.resources.place_report_location_unavailable_message
import ramap.shared.generated.resources.place_report_success_message
import ramap.shared.generated.resources.search_result_empty_message
import ramap.shared.generated.resources.shop_information_report_failure_message
import ramap.shared.generated.resources.shop_information_report_success_message
import kotlin.time.Duration.Companion.milliseconds

class MapViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val shopWaitingSystemRepository: ShopWaitingSystemRepository,
    private val personalizationRepository: PersonalizationRepository,
    private val reportRepository: ShopReportRepository,
    private val loginRepository: LoginRepository,
    private val currentLocationStore: CurrentLocationStore,
    private val notificationSettingsRepository: NotificationSettingsRepository,
) : BaseViewModel<MapUiState, MapIntent, MapSideEffect>(initialState = MapUiState()) {
    private var boundsLoadJob: Job? = null
    private var boundsLoadRequestId = 0L
    private var lastLoadedBounds: MapBounds? = null
    private var searchJob: Job? = null
    private var searchRequestId = 0L
    private val shopDetailCache = mutableMapOf<String, ShopDetail>()

    init {
        viewModelScope.launch { observeSessionState() }
    }

    override suspend fun handleIntent(intent: MapIntent) {
        when (intent) {
            is OnBoundsChanged -> scheduleRamenShopsLoad(intent.bounds)
            is OnMyLocationChanged -> updateMyLocation(intent.location)
            OnInitialLocationFocusConsumed -> consumeInitialLocationFocus()
            is OnShopSelected -> selectShop(intent.shop, intent.shouldFocus)
            is OnShopIdSelected -> selectShop(intent.shopId)
            is OnShopDetailDismissed -> dismissShopDetail()
            is OnSearchResultsDismissed -> dismissSearchResults()
            OnInitialMapRetryClicked -> retryInitialMapLoad()
            is OnQueryChanged -> updateQuery(intent.query)
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

    private suspend fun observeSessionState() {
        loginRepository.sessionState.collectLatest { sessionState ->
            val isAuthenticated = sessionState == LoginSessionState.AUTHENTICATED
            updateAuthState(isAuthenticated)

            if (isAuthenticated) {
                loadPersonalization()
                loadNotificationSubscriptions()
            }
        }
    }

    private fun updateAuthState(isAuthenticated: Boolean) {
        reduce {
            copy(
                isLoggedIn = isAuthenticated,
                accountLabel = if (isAuthenticated) loginRepository.currentUserEmail() else null,
                isDeletingAccount = if (isAuthenticated) isDeletingAccount else false,
                bookmarkedShopIds = if (isAuthenticated) bookmarkedShopIds else emptySet(),
                notificationShopIds = if (isAuthenticated) notificationShopIds else emptySet(),
                hiddenShopIds = if (isAuthenticated) hiddenShopIds else emptySet(),
                personalizationView = if (isAuthenticated) personalizationView else MapPersonalization.ALL,
            )
        }
    }

    private suspend fun selectShop(
        shop: RamenShop,
        shouldFocus: Boolean = true,
    ) {
        val isCurrentSearchResult = shop.id in currentState.search.results
        val cachedDetail = shopDetailCache[shop.id]
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

        loadShopDetail(shop.id)
    }

    private fun dismissShopDetail() {
        reduce {
            copy(
                selectedShop = null,
                shopDetail = null,
                isShopDetailLoading = false,
            )
        }
    }

    private suspend fun selectShop(shopId: String) {
        val cache = shopDetailCache[shopId]
        if (cache != null) {
            selectShop(cache.shop)
            return
        }

        handleResult(
            result = ramenShopRepository.fetchRamenShopsByIds(setOf(shopId)),
            onSuccess = { shops -> shops[shopId]?.let { selectShop(it) } },
            onError = {},
        )
    }

    private fun updateMyLocation(location: Location) {
        currentLocationStore.update(location)
        reduce {
            copy(
                currentLocation = location,
                initialLocationFocus = initialLocationFocus.request(location),
            )
        }
    }

    private fun consumeInitialLocationFocus() {
        reduce { copy(initialLocationFocus = initialLocationFocus.consume()) }
    }

    private fun dismissSearchResults() {
        reduce { copy(search = search.dismissResults()) }
    }

    private suspend fun updateQuery(query: String) {
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
            handleSingleSearchResult(currentState.searchResultShops.singleShopOrNull())
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
        if (!isLoggedInOrShowGuide()) return

        val isBookmarked = shop.id in currentState.bookmarkedShopIds
        reduceBookmarkState(shop.id)
        postBookmark(shop.id, isBookmarked)
    }

    private suspend fun loadNotificationSubscriptions() {
        val repository = notificationSettingsRepository
        handleResult(
            result = repository.fetchSubscribedShopIds(),
            onSuccess = { ids -> reduce { copy(notificationShopIds = ids) } },
        )
    }

    private suspend fun toggleShopNotification(shop: RamenShop) {
        if (!isLoggedInOrShowGuide()) return
        val isHiddenShop = shop.id in currentState.hiddenShopIds
        if (isHiddenShop) {
            showToast(Res.string.hidden_shop_notification_unavailable_message)
            return
        }

        val wasEnabled = shop.id in currentState.notificationShopIds
        reduceNotificationShop(shop.id)
        postShopNotification(shop.id, wasEnabled)
    }

    private suspend fun postShopNotification(
        shopId: String,
        wasEnabled: Boolean,
    ) {
        handleResult(
            result = notificationSettingsRepository.updateShopNotification(shopId, !wasEnabled),
            onError = { handleShopNotificationFailure(shopId) },
        )
    }

    private fun handleShopNotificationFailure(shopId: String) {
        reduceNotificationShop(shopId)
        showPersonalizationUpdateFailure()
    }

    private fun reduceNotificationShop(shopId: String) {
        reduce {
            copy(
                notificationShopIds =
                    if (shopId in notificationShopIds) notificationShopIds - shopId else notificationShopIds + shopId,
            )
        }
    }

    private suspend fun postBookmark(
        shopId: String,
        isBookmarked: Boolean,
    ) {
        handleResult(
            result = updateBookmarkPersonalization(shopId, isBookmarked),
            onSuccess = {
                if (!isBookmarked) loadPersonalizedShops(setOf(shopId))
            },
            onError = { handleBookmarkFailure(shopId) },
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
    ): RamapResult<Unit> =
        if (isBookmarked) {
            personalizationRepository.removeBookmark(shopId)
        } else {
            personalizationRepository.addBookmark(shopId)
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
        val shouldRemoveBookmark = shop.id in currentState.bookmarkedShopIds
        val shouldDisableNotification = shop.id in currentState.notificationShopIds
        val previousBookmarkedShopIds = currentState.bookmarkedShopIds
        val previousNotificationShopIds = currentState.notificationShopIds
        val previousHiddenShopIds = currentState.hiddenShopIds
        val previousSelectedShop = currentState.selectedShop
        val previousSearch = currentState.search

        reduceHideShopState(shop.id, shouldRemoveBookmark, shouldDisableNotification)
        handleResult(
            result = persistHiddenShop(shop.id, shouldRemoveBookmark, shouldDisableNotification),
            onSuccess = { showHiddenShopSuccess() },
            onError = {
                restoreHiddenShopState(
                    previousBookmarkedShopIds,
                    previousNotificationShopIds,
                    previousHiddenShopIds,
                    previousSelectedShop,
                    previousSearch,
                )
                showPersonalizationUpdateFailure()
            },
        )
    }

    private fun showHiddenShopSuccess() {
        showToast(Res.string.hide_shop_success_message)
    }

    private suspend fun persistHiddenShop(
        shopId: String,
        shouldRemoveBookmark: Boolean,
        shouldDisableNotification: Boolean,
    ): RamapResult<Unit> {
        val notificationRepository = notificationSettingsRepository
        if (shouldDisableNotification) {
            val notificationResult = notificationRepository.updateShopNotification(shopId, false)
            if (notificationResult is RamapResult.Error) return notificationResult
        }

        val hideResult =
            personalizationRepository.hideShop(shopId, removeBookmark = shouldRemoveBookmark)
        if (hideResult is RamapResult.Error && shouldDisableNotification) {
            notificationRepository.updateShopNotification(shopId, true)
        }
        return hideResult
    }

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
        notificationShopIds: Set<String>,
        hiddenShopIds: Set<String>,
        selectedShop: RamenShop?,
        search: SearchUiState,
    ) {
        reduce {
            copy(
                bookmarkedShopIds = bookmarkedShopIds,
                notificationShopIds = notificationShopIds,
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
        shouldDisableNotification: Boolean = false,
    ) {
        reduce {
            val shouldCloseSelectedShop = selectedShop?.id == shopId

            copy(
                hiddenShopIds = hiddenShopIds + shopId,
                bookmarkedShopIds = if (shouldRemoveBookmark) bookmarkedShopIds - shopId else bookmarkedShopIds,
                notificationShopIds =
                    if (shouldDisableNotification) notificationShopIds - shopId else notificationShopIds,
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
        if (view != MapPersonalization.ALL && !isLoggedInOrShowGuide()) return

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

    private fun toggleBookmarkedView() {
        if (!isLoggedInOrShowGuide()) return

        val nextView =
            if (currentState.personalizationView == MapPersonalization.BOOKMARKED) {
                MapPersonalization.ALL
            } else {
                MapPersonalization.BOOKMARKED
            }
        reduce {
            copy(
                personalizationView = nextView,
                selectedShop =
                    selectedShop?.takeIf { shop ->
                        when (nextView) {
                            MapPersonalization.ALL -> shop.id !in hiddenShopIds
                            MapPersonalization.BOOKMARKED -> shop.id in bookmarkedShopIds
                            MapPersonalization.HIDDEN -> shop.id in hiddenShopIds
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

        handleResult(
            result =
                reportRepository.submitShopInformationReport(
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

    private fun showShopInformationReportSuccess() {
        showToast(Res.string.shop_information_report_success_message)
    }

    private fun showShopInformationReportFailure() {
        showToast(Res.string.shop_information_report_failure_message, ToastType.ERROR)
    }

    private suspend fun submitUnregisteredPlaceReport(placeUrl: String) {
        val extractedPlaceUrl = PlaceReportTextParser.extractSupportedUrl(placeUrl)
        if (extractedPlaceUrl == null) {
            showToast(Res.string.place_report_invalid_url_message, ToastType.ERROR)
            return
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

    private suspend fun findExistingShop(placeUrl: String): RamenShop? {
        val placeName = PlaceReportTextParser.extractSharedPlaceName(placeUrl) ?: return null
        return when (
            val result =
                ramenShopRepository.searchRamenShops(SearchQuery(placeName), SEARCH_RESULT_LIMIT)
        ) {
            is RamapResult.Success ->
                result.data.values.firstOrNull {
                    PlaceReportTextParser.matchesSharedPlace(placeUrl, it)
                }

            is RamapResult.Error -> null
        }
    }

    private suspend fun submitCurrentLocationReport() {
        val location = currentState.currentLocation
        if (location == null) {
            showToast(Res.string.place_report_location_unavailable_message, ToastType.ERROR)
            return
        }

        submitUnregisteredPlaceReport(UnregisteredPlaceReport(location = location))
    }

    private suspend fun submitUnregisteredPlaceReport(report: UnregisteredPlaceReport) {
        handleResult(
            result = reportRepository.submitUnregisteredPlaceReport(report),
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

        searchJob =
            viewModelScope.launch {
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

    private suspend fun handleSearchSuccess(
        query: SearchQuery,
        shops: RamenShops,
    ) {
        reduceSearchResult(query, shops)
        if (shops.isEmpty()) {
            showToast(Res.string.search_result_empty_message)
        }
        handleSingleSearchResult(currentState.searchResultShops.singleShopOrNull())
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

    private suspend fun loadShopDetail(shopId: String) {
        handleResult(
            result = retryOnce { fetchShopDetail(shopId) },
            onSuccess = { detail -> handleShopDetailSuccess(shopId, detail) },
            onError = { handleShopDetailFailure(shopId) },
        )
    }

    private fun handleShopDetailSuccess(
        shopId: String,
        detail: ShopDetail,
    ) {
        shopDetailCache[shopId] = detail
        val selectedShop = currentState.selectedShop ?: return
        if (selectedShop.id != shopId) return

        reduce {
            copy(
                selectedShop = detail.shop.copy(isVisible = selectedShop.isVisible),
                shopWaiting = shopWaiting + (shopId to detail.waitingSystem),
                shopDetail = detail,
                isShopDetailLoading = false,
            )
        }
    }

    private fun handleShopDetailFailure(shopId: String) {
        if (currentState.selectedShop?.id == shopId) {
            reduce { copy(shopDetail = null, isShopDetailLoading = false) }
            showDataLoadFailure()
        }
    }

    private suspend fun fetchShopDetail(shopId: String): RamapResult<ShopDetail> =
        coroutineScope {
            val shopResult = async { ramenShopRepository.fetchRamenShopsByIds(setOf(shopId)) }
            val waitingResult = async { shopWaitingSystemRepository.fetchShopWaitingSystem(shopId) }
            val eventResult = async { ramenShopRepository.fetchActiveShopEvent(shopId) }

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
                            when (val event = eventResult.await()) {
                                is RamapResult.Error ->
                                    RamapResult.Success(
                                        ShopDetail(
                                            shop,
                                            waiting.data,
                                            null,
                                        ),
                                    )

                                is RamapResult.Success ->
                                    RamapResult.Success(
                                        ShopDetail(
                                            shop,
                                            waiting.data,
                                            event.data,
                                        ),
                                    )
                            }
                    }
                }
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
     * 마지막 성공 조회 영역이 현재 화면을 포함하지 않을 때만 확장 영역을 조회한다.
     *
     * 요청 취소를 협조하지 못한 오래된 작업이 늦게 끝나더라도 최신 request id와 다르면
     * 결과를 버리고, 조회 결과가 기존 UI 상태와 같으면 state 갱신도 생략해 마커 재렌더링을 줄인다.
     */
    private suspend fun loadRamenShops(
        bounds: MapBounds,
        requestId: Long,
    ) {
        if (lastLoadedBounds?.contains(bounds) == true) return

        val expandedBounds = bounds.expandBy(BOUNDS_PREFETCH_RATIO)

        val isInitialLoad = currentState.initialMapLoadState != InitialMapLoadState.CONTENT
        val result =
            if (isInitialLoad) {
                retryOnce { ramenShopRepository.fetchRamenShops(expandedBounds) }
            } else {
                ramenShopRepository.fetchRamenShops(expandedBounds)
            }
        if (requestId != boundsLoadRequestId) return

        handleResult(
            result = result,
            onSuccess = { shops -> handleRamenShopsLoadSuccess(expandedBounds, shops) },
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
        private const val BOUNDS_PREFETCH_RATIO = 0.5
        private const val BOUNDS_LOAD_DEBOUNCE_MILLIS = 350L
        private const val SEARCH_RESULT_LIMIT = 50
    }
}
