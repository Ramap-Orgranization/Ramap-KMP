package com.peto.ramap.ui.map.contract

import com.peto.ramap.core.base.State
import com.peto.ramap.core.config.DefaultMapConfig
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShopFilter
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.ShopWaitingSystem
import com.peto.ramap.domain.model.nearestTo
import com.peto.ramap.ui.map.model.MapPersonalization
import com.peto.ramap.ui.map.model.SearchUiState

data class MapUiState(
    /**
     * 지도에서 조회 가능한 전체 라멘 매장 목록.
     */
    val shops: RamenShops = RamenShops(emptyMap()),
    /**
     * 상세 바텀시트에 표시할 현재 선택 매장.
     */
    val selectedShop: RamenShop? = null,
    /**
     * 검색창 입력값, 검색 결과, 검색 결과의 소비 상태.
     */
    val search: SearchUiState = SearchUiState(),
    /**
     * 매장 id별 웨이팅 시스템 정보.
     */
    val shopWaiting: Map<String, ShopWaitingSystem?> = emptyMap(),
    /**
     * 지도와 검색 결과에 적용 중인 매장 필터.
     */
    val filters: RamenShopFilter = RamenShopFilter(),
    /**
     * 현재 지도 카메라가 보고 있는 영역.
     */
    val bounds: MapBounds = DefaultMapConfig.bounds,
    /**
     * 마지막으로 확인된 사용자 위치.
     */
    val currentLocation: Location? = null,
    val currentAddress: String? = null,
    /**
     * 사용자가 북마크한 매장 id 목록.
     */
    val bookmarkedShopIds: Set<String> = emptySet(),
    /**
     * 사용자가 숨김 처리한 매장 id 목록.
     */
    val hiddenShopIds: Set<String> = emptySet(),
    /**
     * 전체, 북마크, 숨김 매장 중 현재 지도에 표시할 개인화 모드.
     */
    val personalizationView: MapPersonalization = MapPersonalization.ALL,
    /**
     * 현재 사용자의 로그인 여부.
     */
    val isLoggedIn: Boolean = false,
    /**
     * 계정 영역에 표시할 사용자 식별 라벨.
     */
    val accountLabel: String? = null,
    val isDeletingAccount: Boolean = false,
) : State {
    /**
     * 검색 결과 리스트 바텀시트에 표시할 매장 목록.
     *
     * [SearchUiState.results]는 마커 렌더링을 위해 id 기반 컬렉션으로 유지하고,
     * 리스트 UI에서는 순회하기 쉬운 [List] 형태로 변환해 사용한다.
     */
    val searchResultShops: List<RamenShop>
        get() {
            val shops = displaySearchResults.values.toList()
            val nearestShop = shops.nearestTo(currentLocation) ?: return shops

            return listOf(nearestShop) + shops.filterNot { shop -> shop.id == nearestShop.id }
        }

    /**
     * 검색 결과 대신 사용자에게 안내할 메시지 상태.
     *
     * 현재 검색어에 대한 결과가 로드된 뒤, 전체 보기 화면에서만 안내를 판단한다.
     * 검색 결과가 없거나 필터 적용 후 표시할 매장이 없을 때 적절한 [SearchResultGuide]를 반환한다.
     */
    val searchResultGuide: SearchResultGuide?
        get() {
            if (!hasLoadedSearchResultsForCurrentQuery) return null
            if (personalizationView != MapPersonalization.ALL) return null
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
            return if (hasLoadedSearchResultsForCurrentQuery) {
                RamenShops(displayFilteredShops + displaySearchResults)
            } else {
                displayFilteredShops
            }
        }

    /**
     * 검색 결과 리스트 바텀시트를 보여줄지 여부.
     *
     * 매장 상세가 열려 있지 않고 검색어가 있으며, 선택 가능한 검색 결과가 여러 개일 때만
     * 리스트를 노출한다.
     */
    val showSearchResults: Boolean
        get() =
            selectedShop == null &&
                !search.isResultsDismissed &&
                personalizationView == MapPersonalization.ALL &&
                search.input.isNotBlank() &&
                searchResultShops.size > 1

    /**
     * 지도 화면의 바텀시트를 열지 여부.
     *
     * 선택 매장 상세 또는 다중 검색 결과 리스트 중 하나라도 표시할 내용이 있으면 true가 된다.
     */
    val showBottomSheet: Boolean
        get() = selectedShop != null || showSearchResults

    /**
     * 지도 카메라가 포커스해야 할 매장 목록.
     *
     * 상세 화면에서는 선택 매장 1개를 중심으로 이동하고,
     * 검색 결과가 있으면 단일 결과는 중심으로, 여러 결과는 한 화면에 보이도록 이동한다.
     */
    val focusShops: List<RamenShop>
        get() =
            when {
                selectedShop != null -> listOf(selectedShop)
                shouldFocusSearchResults -> searchResultShops
                else -> emptyList()
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
     * 개인화 보기 모드와 카테고리 필터가 적용된 전체 매장 목록.
     */
    private val filteredShops: RamenShops
        get() =
            when (personalizationView) {
                MapPersonalization.ALL -> shops.filterNotHidden(hiddenShopIds)
                MapPersonalization.BOOKMARKED -> shops.filterByShopIds(bookmarkedShopIds)
                MapPersonalization.HIDDEN -> shops.filterByShopIds(hiddenShopIds)
            }.filterByCategory(filters)

    /**
     * 개인화 보기 모드와 카테고리 필터가 적용된 검색 결과 목록.
     */
    private val filteredSearchResults: RamenShops
        get() =
            when (personalizationView) {
                MapPersonalization.ALL -> search.results
                MapPersonalization.BOOKMARKED -> search.results.filterByShopIds(bookmarkedShopIds)
                MapPersonalization.HIDDEN -> search.results.filterByShopIds(hiddenShopIds)
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
}
