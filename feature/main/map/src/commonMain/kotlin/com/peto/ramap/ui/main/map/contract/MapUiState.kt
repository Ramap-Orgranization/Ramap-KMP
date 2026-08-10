package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShopFilter
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.WaitingSystem
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState
import com.peto.ramap.ui.main.map.config.DefaultMapConfig
import com.peto.ramap.ui.main.map.model.CameraPosition
import com.peto.ramap.ui.main.map.model.LocationFocusStatus
import com.peto.ramap.designsystem.shop.model.ShopDetailSheetUiState
import com.peto.ramap.ui.main.map.search.SearchResultGuide
import com.peto.ramap.ui.main.map.search.SearchUiModel

data class MapUiState(
    val shopDetailState: ShopDetailSheetUiState = ShopDetailSheetUiState.Closed,
    /** 지도 화면의 작업별 로딩 카운트. */
    override val loadState: LoadState = LoadState(),
    /**
     * 지도에서 조회 가능한 전체 라멘 매장 목록.
     */
    val shops: RamenShops = RamenShops(emptyMap()),
    /**
     * 매장 상세보기시 지도 포커스 여부
     */
    val shouldFocusSelectedShop: Boolean = true,
    /**
     * 검색창 입력값, 검색 결과, 검색 결과의 소비 상태.
     */
    val search: SearchUiModel = SearchUiModel(),
    val recentSearches: List<String> = emptyList(),
    val recentlyViewedShops: RamenShops = RamenShops(emptyMap()),
    /**
     * 매장 id별 웨이팅 시스템 정보.
     */
    val shopWaiting: Map<String, WaitingSystem?> = emptyMap(),
    /**
     * 지도와 검색 결과에 적용 중인 매장 필터.
     */
    val filters: RamenShopFilter = RamenShopFilter(),
    /**
     * 현재 지도 카메라가 보고 있는 영역.
     */
    val bounds: MapBounds = DefaultMapConfig.bounds,
    val hasViewportLoadFailed: Boolean = false,
    /**
     * 현재 지도 영역(bounds) 기준 매장 조회가 진행 중인지 여부.
     */
    val cameraPosition: CameraPosition? = null,
    /**
     * 마지막으로 확인된 사용자 위치.
     */
    val currentLocation: Location? = null,
    val locationFocusStatus: LocationFocusStatus = LocationFocusStatus.AwaitingLocationStatus,
    /**
     * 사용자가 북마크한 매장 id 목록.
     */
    val bookmarkedShopIds: Set<String> = emptySet(),
    /**
     * 사용자가 일림 설정한 매장 id 목록.
     */
    val notificationShopIds: Set<String> = emptySet(),
    /**
     * 사용자가 숨김 처리한 매장 id 목록.
     */
    val hiddenShopIds: Set<String> = emptySet(),
    /**
     * 북마크한 매장만 지도에 표시하는지 여부.
     */
    val isBookmarkedView: Boolean = false,
    /**
     * 현재 사용자의 로그인 여부.
     */
    val isLoggedIn: Boolean = false,
) : State,
    LoadableState<MapUiState> {
    val selectedShop: RamenShop?
        get() =
            when (val state = shopDetailState) {
                ShopDetailSheetUiState.Closed -> null
                is ShopDetailSheetUiState.Loading -> state.shop
                is ShopDetailSheetUiState.Content -> state.detail.shop
                is ShopDetailSheetUiState.Error -> state.shop
            }

    val shopDetail: ShopDetail?
        get() = (shopDetailState as? ShopDetailSheetUiState.Content)?.detail

    val isShopDetailLoading: Boolean
        get() = shopDetailState is ShopDetailSheetUiState.Loading

    val isSearchLoading: Boolean
        get() = loadState.isLoading(MapLoadKey.Search)

    val hasShopDetailLoadFailed: Boolean
        get() = shopDetailState is ShopDetailSheetUiState.Error

    /** 로딩 카운트만 교체한 새 지도 UI 상태를 반환한다. */
    override fun withLoadingState(loadState: LoadState): MapUiState = copy(loadState = loadState)

    /**
     * 검색 결과 리스트 바텀시트에 표시할 매장 목록.
     *
     * 현재 위치와 가까운 매장을 먼저 표시한다.
     */
    val searchResultShops: RamenShops
        get() = displaySearchResults.nearestFirstTo(currentLocation)

    val placeSearchResults: PlaceSearchResults
        get() = search.placeResults.nearestFirstTo(cameraPosition?.center ?: defaultCameraCenter)

    /**
     * 검색 결과 대신 사용자에게 안내할 메시지 상태.
     *
     * 현재 검색어에 대한 결과가 로드된 뒤, 전체 보기 화면에서만 안내를 판단한다.
     * 검색 결과가 없거나 필터 적용 후 표시할 매장이 없을 때 적절한 [SearchResultGuide]를 반환한다.
     */
    val searchResultGuide: SearchResultGuide?
        get() {
            if (!hasLoadedSearchResultsForCurrentQuery) return null
            if (isBookmarkedView) return null
            if (placeSearchResults.isNotEmpty()) return null
            if (search.results.isEmpty()) return SearchResultGuide.SEARCH_EMPTY
            if (
                displaySearchResults.isNotEmpty() &&
                displaySearchResults.values.all { !it.isVisible }
            ) {
                return SearchResultGuide.HIDDEN_ONLY
            }
            if (displaySearchResults.isEmpty() && filters.isNotEmpty()) {
                return SearchResultGuide.QUERY_AND_FILTER_EMPTY
            }
            if (displaySearchResults.isEmpty()) return SearchResultGuide.FILTER_EMPTY

            return null
        }

    /**
     * 지도 마커로 렌더링할 매장 목록.
     *
     * 검색어가 없거나 현재 입력값에 대한 검색 결과가 아직 도착하지 않았으면 [shops]를 유지한다.
     * 현재 입력값에 대응하는 검색 결과가 도착한 뒤에는 지도 영역 매장과 검색 결과를 함께 보여준다.
     */
    val markerShops: RamenShops
        get() {
            val selectedMarkerShop =
                selectedShop
                    ?.let { shop -> mapOf(shop.id to shop) }
                    .orEmpty()

            return if (hasLoadedSearchResultsForCurrentQuery) {
                RamenShops(displayFilteredShops + displaySearchResults + selectedMarkerShop)
            } else {
                RamenShops(displayFilteredShops + selectedMarkerShop)
            }
        }

    /**
     * 검색 결과 리스트 바텀시트를 보여줄지 여부.
     *
     * 매장 상세가 열려 있지 않고 검색어가 있으며, 선택 가능한 검색 결과가 여러 개이거나
     * 빈 검색 결과를 제외한 안내가 있을 때만 리스트를 노출한다.
     */
    val showSearchResults: Boolean
        get() =
            selectedShop == null &&
                !search.isResultsDismissed &&
                !isBookmarkedView &&
                search.input.isNotBlank() &&
                (
                    (
                        searchResultGuide != null &&
                            searchResultGuide != SearchResultGuide.SEARCH_EMPTY &&
                            searchResultGuide != SearchResultGuide.HIDDEN_ONLY
                    ) ||
                        searchResultShops.size > 1 ||
                        placeSearchResults.size > 1
                )

    /**
     * 지도 화면의 바텀시트를 열지 여부.
     *
     * 선택 매장 상세 또는 다중 검색 결과 리스트 중 하나라도 표시할 내용이 있으면 true가 된다.
     */
    val showBottomSheet: Boolean
        get() = selectedShop != null || showSearchResults || hasShopDetailLoadFailed

    /**
     * 지도 카메라가 포커스해야 할 매장 목록.
     *
     * 마커 외 경로에서 연 상세 화면은 선택 매장 1개를 중심으로 이동하고,
     * 검색 결과가 있으면 단일 결과는 중심으로, 여러 결과는 한 화면에 보이도록 이동한다.
     */
    val focusShops: RamenShops
        get() {
            val shop = selectedShop
            return when {
                shop != null && shouldFocusSelectedShop -> RamenShops(listOf(shop))
                shouldFocusSearchResults -> searchResultShops
                else -> RamenShops(emptyMap())
            }
        }

    /**
     * 가장 가까운 검색 결과로 지도 카메라를 이동해야 하는지 여부.
     *
     * 검색 결과 포커스가 필요한 상태에서 결과가 여러 개일 때 true가 된다.
     */
    val shouldFocusNearestSearchResult: Boolean
        get() = shouldFocusSearchResults && searchResultShops.size > 1

    val focusRequestKey: Long
        get() = search.focusRequestKey

    val placeFocusLocation: Location?
        get() = search.placeFocusLocation

    val placeFocusRequestKey: Long
        get() = search.placeFocusRequestKey

    val initialFocusLocation: Location?
        get() = (locationFocusStatus as? LocationFocusStatus.Pending)?.location

    val shouldBootstrapLocationFocusStatus: Boolean
        get() = locationFocusStatus is LocationFocusStatus.AwaitingLocationStatus

    /**
     * 검색 결과에 맞춰 지도 카메라 포커스를 수행할 수 있는 상태인지 여부.
     */
    private val shouldFocusSearchResults: Boolean
        get() =
            selectedShop == null &&
                search.input.isNotBlank() &&
                searchResultShops.isNotEmpty() &&
                !search.isResultFocusConsumed

    /**
     * 현재 입력된 검색어에 대한 검색 결과가 로드되어 있는지 여부.
     */
    private val hasLoadedSearchResultsForCurrentQuery: Boolean
        get() {
            return search.hasLoadedResultsForInput
        }

    /**
     * 북마크 보기 여부와 카테고리 필터가 적용된 전체 매장 목록.
     */
    private val filteredShops: RamenShops
        get() =
            if (isBookmarkedView) {
                shops.filterByShopIds(bookmarkedShopIds)
            } else {
                shops.filterNotHidden(hiddenShopIds)
            }.filterByCategory(filters)

    /**
     * 북마크 보기 여부와 카테고리 필터가 적용된 검색 결과 목록.
     */
    private val filteredSearchResults: RamenShops
        get() =
            if (isBookmarkedView) {
                search.results.filterByShopIds(bookmarkedShopIds)
            } else {
                search.results
            }.filterByCategory(filters)

    /**
     * 숨김 매장의 표시 상태까지 반영한 검색 결과 목록.
     */
    private val displaySearchResults: RamenShops
        get() = filteredSearchResults.markHidden(hiddenShopIds)

    /**
     * 숨김 매장의 표시 상태까지 반영한 전체 매장 목록.
     */
    private val displayFilteredShops: RamenShops
        get() = filteredShops.markHidden(hiddenShopIds)

    private val defaultCameraCenter =
        Location(
            lat = DefaultMapConfig.LATITUDE,
            lng = DefaultMapConfig.LONGITUDE,
        )
}
