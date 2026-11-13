package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShopFilter
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.ShopHoursFilter
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState
import com.peto.ramap.ui.main.map.model.CameraPosition
import com.peto.ramap.ui.main.map.model.PlaceFocusRequest
import com.peto.ramap.ui.main.map.model.ShopFocusRequest
import com.peto.ramap.ui.main.map.model.location.LocationFocusStatus
import com.peto.ramap.ui.main.map.model.search.SearchResultGuide
import com.peto.ramap.ui.main.map.model.search.SearchUiModel
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class MapUiState(
    val currentDateTime: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul")),
    val selectedShopId: String? = null,
    val shopSelectionKey: Long = 0,
    val selectedShop: RamenShop? = null,
    /** 지도 화면의 작업별 로딩 카운트. */
    override val loadState: LoadState = LoadState(),
    /**
     * 지도에서 조회 가능한 전체 라멘 매장 목록.
     */
    val shops: RamenShops = RamenShops(emptyMap()),
    /**
     * 아직 소비하지 않은 선택 매장의 카메라 이동 요청.
     */
    val selectedShopFocusRequest: ShopFocusRequest? = null,
    val placeFocusRequest: PlaceFocusRequest? = null,
    /**
     * 검색창 입력값, 검색 결과, 검색 결과의 소비 상태.
     */
    val search: SearchUiModel = SearchUiModel(),
    val recentSearches: List<String> = emptyList(),
    val recentlyViewedShops: RamenShops = RamenShops(emptyMap()),
    /**
     * 지도와 검색 결과에 적용 중인 매장 필터.
     */
    val filters: RamenShopFilter = RamenShopFilter(),
    val isRecommendationEnabled: Boolean = false,
    val recommendationToggleKey: Long = 0,
    val recommendedShops: RamenShops? = null,
    /**
     * 운영 공지 목록
     */
    val operatingNotices: List<OperatingNotice> = emptyList(),
    /**
     * 현재 지도 카메라가 보고 있는 영역.
     */
    val bounds: MapBounds = MapBounds(),
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
    val bookmarkUpdatingShopIds: Set<String> = emptySet(),
    /**
     * 사용자가 일림 설정한 매장 id 목록.
     */
    val notificationShopIds: Set<String> = emptySet(),
    /**
     * 북마크한 매장만 지도에 표시하는지 여부.
     */
    val isBookmarkedView: Boolean = false,
    /**
     * 현재 사용자의 로그인 여부.
     */
    val isLoggedIn: Boolean = false,
) : LoadableState<MapUiState> {
    val hasActiveFilters: Boolean
        get() = isRecommendationEnabled || isBookmarkedView || filters.isNotEmpty()

    val hasHoursFilter: Boolean
        get() = filters.hours != ShopHoursFilter.All

    val isVisitTimeSelected: Boolean
        get() = filters.hours is ShopHoursFilter.VisitTime

    val isSearchLoading: Boolean
        get() = loadState.isLoading(MapLoadKey.Search)

    val isRecommendationLoading: Boolean
        get() = loadState.isLoading(MapLoadKey.Recommendation)

    val showRecommendationList: Boolean
        get() = isRecommendationEnabled && search.input.isBlank() && !showBottomSheet

    val recommendationListShops: List<RamenShop>
        get() =
            recommendedShops
                ?.let(::applyShopFilters)
                ?.orderedByDistanceThenName(currentLocation)
                .orEmpty()

    val showsLoadingOverlay: Boolean
        get() = isSearchLoading

    /** 로딩 카운트만 교체한 새 지도 UI 상태를 반환한다. */
    override fun withLoadingState(loadState: LoadState): MapUiState = copy(loadState = loadState)

    /**
     * 검색 결과 리스트 바텀시트에 표시할 매장 목록.
     *
     * 서버의 매장명 관련도·거리 순서를 유지한다.
     */
    val searchResultShops: RamenShops
        get() =
            if (search.showsShopResults) {
                filteredSearchResults
            } else {
                RamenShops(emptyMap())
            }

    val hasSearchResult: Boolean
        get() = searchResultShops.isNotEmpty()

    /**
     * 검색 결과 대신 사용자에게 안내할 메시지 상태.
     *
     * 현재 검색어에 대한 결과가 로드된 뒤, 현재 보기 모드에서 안내를 판단한다.
     * 검색 결과가 없거나 필터 적용 후 표시할 매장이 없을 때 적절한 [SearchResultGuide]를 반환한다.
     */
    val searchResultGuide: SearchResultGuide?
        get() {
            if (!search.showsShopResults) return null
            if (search.results.isEmpty()) return SearchResultGuide.SearchEmpty
            if (filteredSearchResults.isEmpty() && filters.isNotEmpty()) {
                return SearchResultGuide.QueryAndFilterEmpty
            }
            if (filteredSearchResults.isEmpty()) return SearchResultGuide.FilterEmpty

            return null
        }

    /**
     * 지도 마커로 렌더링할 매장 목록.
     *
     * 검색어가 없거나 현재 입력값에 대한 검색 결과가 아직 도착하지 않았으면 [shops]를 유지한다.
     * 현재 입력값에 대응하는 검색 결과가 도착한 뒤에는 검색 결과 매장만 보여준다.
     */
    val markerShops: RamenShops
        get() = markerShops(includeSelectedShop = true)

    /** 닫힐 예정인 상세의 마커를 제외한 필터 결과도 같은 규칙으로 계산한다. */
    fun markerShops(includeSelectedShop: Boolean): RamenShops {
        val selectedMarkerShop =
            selectedShop
                ?.takeIf { shop ->
                    includeSelectedShop &&
                        (!isRecommendationEnabled || (shop.isVisible && shop.isRecommended)) &&
                        shop.isOpened(filters, currentDateTime, operatingNotices)
                }?.let { shop -> mapOf(shop.id to shop) }
                .orEmpty()

        val shopsToShow = if (search.showsShopResults) filteredSearchResults else filteredShops
        return RamenShops(shopsToShow + selectedMarkerShop)
    }

    internal fun hoursFilterResultCount(
        hours: ShopHoursFilter,
        includeSelectedShop: Boolean = true,
    ): Int {
        val preview = if (hours == filters.hours) this else copy(filters = filters.copy(hours = hours))
        return when {
            search.showsShopResults -> preview.searchResultShops.size
            isRecommendationEnabled -> recommendedShops?.let(preview::applyShopFilters)?.size ?: 0
            else -> preview.markerShops(includeSelectedShop).values.count { bounds.contains(it.location) }
        }
    }

    /**
     * 검색 결과 리스트 바텀시트를 보여줄지 여부.
     */
    val showSearchResults: Boolean
        get() {
            if (!search.showsShopResults || search.isResultsDismissed) return false
            if (hasHoursFilter && search.results.isNotEmpty()) return true

            val minimumShopCount = if (isRecommendationEnabled) 1 else 2
            return search.places.isNotEmpty() || searchResultShops.size >= minimumShopCount
        }

    /**
     * 지도 화면의 바텀시트를 열지 여부.
     *
     * 선택 매장 상세 또는 다중 검색 결과 리스트 중 하나라도 표시할 내용이 있으면 true가 된다.
     */
    val showBottomSheet: Boolean
        get() = selectedShopId != null

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

    val shouldFocusSelectedShop: Boolean
        get() = selectedShopFocusRequest?.shopId?.let { it == selectedShop?.id } == true

    /**
     * 서버가 우선한 첫 검색 결과로 지도 카메라를 이동해야 하는지 여부.
     *
     * 검색 결과 포커스가 필요한 상태에서 결과가 여러 개일 때 true가 된다.
     */
    val shouldFocusFirstSearchResult: Boolean
        get() = shouldFocusSearchResults && searchResultShops.size > 1

    val focusRequestKey: Long
        get() = selectedShopFocusRequest?.takeIf { shouldFocusSelectedShop }?.let { -it.sequence } ?: search.focusRequestKey

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
                search.places.isEmpty() &&
                !search.isResultFocusConsumed

    /**
     * 북마크 보기 여부와 카테고리 필터가 적용된 전체 매장 목록.
     */
    private val filteredShops: RamenShops
        get() = applyShopFilters(if (isRecommendationEnabled) recommendedShops ?: shops else shops)

    /**
     * 북마크 보기 여부와 카테고리 필터가 적용된 검색 결과 목록.
     */
    private val filteredSearchResults: RamenShops
        get() = applyShopFilters(search.results)

    private fun applyShopFilters(shops: RamenShops): RamenShops {
        val scopedShops = if (isRecommendationEnabled) shops.visibleRecommendations() else shops
        val savedShops = if (isBookmarkedView) scopedShops.filterByShopIds(bookmarkedShopIds) else scopedShops
        return savedShops.filterByOpenStatus(filters, currentDateTime, operatingNotices)
    }
}
