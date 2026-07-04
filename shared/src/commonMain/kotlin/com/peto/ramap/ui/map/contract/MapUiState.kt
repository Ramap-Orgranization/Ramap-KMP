package com.peto.ramap.ui.map.contract

import com.peto.ramap.core.base.State
import com.peto.ramap.core.config.DefaultMapConfig
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShopFilter
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.SearchQuery
import com.peto.ramap.domain.model.ShopWaitingSystem
import com.peto.ramap.ui.map.model.MapPersonalization

data class MapUiState(
    val shops: RamenShops = RamenShops(emptyMap()),
    val selectedShop: RamenShop? = null,
    val query: String = "",
    val shopWaiting: Map<String, ShopWaitingSystem?> = emptyMap(),
    val searchResults: RamenShops = RamenShops(emptyMap()),
    val searchResultsQuery: SearchQuery? = null,
    val isSearchResultsDismissed: Boolean = false,
    val isSearchResultFocusConsumed: Boolean = false,
    val filters: RamenShopFilter = RamenShopFilter(),
    val bounds: MapBounds = DefaultMapConfig.bounds,
    val clusterBounds: MapBounds = DefaultMapConfig.bounds,
    val bookmarkedShopIds: Set<String> = emptySet(),
    val hiddenShopIds: Set<String> = emptySet(),
    val personalizationView: MapPersonalization = MapPersonalization.ALL,
    val isLoggedIn: Boolean = false,
    val accountLabel: String? = null,
) : State {
    /**
     * 검색 결과 리스트 바텀시트에 표시할 매장 목록.
     *
     * [searchResults]는 마커 렌더링을 위해 id 기반 컬렉션으로 유지하고,
     * 리스트 UI에서는 순회하기 쉬운 [List] 형태로 변환해 사용한다.
     */
    val searchResultShops: List<RamenShop>
        get() = displaySearchResults.values.toList()

    /**
     * 지도 마커로 렌더링할 매장 목록.
     *
     * 검색어가 없거나 현재 입력값에 대한 검색 결과가 아직 도착하지 않았으면 [shops]를 유지한다.
     * 현재 입력값에 대응하는 검색 결과가 도착한 뒤에만 [searchResults]로 전환해
     * 검색 debounce 구간에서 기존 마커가 사라졌다가 다시 추가되는 현상을 막는다.
     */
    val markerShops: RamenShops
        get() {
            val normalizedQuery = SearchQuery(query).normalizeShopSearchQuery()

            return if (normalizedQuery.value.isNotBlank() && searchResultsQuery == normalizedQuery) {
                displaySearchResults
            } else {
                filteredShops
            }
        }

    /**
     * 검색 결과 리스트 바텀시트를 보여줄지 여부.
     *
     * 매장 상세가 열려 있지 않고 검색어가 있으며, 결과가 여러 개일 때만 리스트를 노출한다.
     */
    val showSearchResults: Boolean
        get() =
            selectedShop == null &&
                !isSearchResultsDismissed &&
                query.isNotBlank() &&
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

    val shouldFocusNearestSearchResult: Boolean
        get() = shouldFocusSearchResults && searchResultShops.size > 1

    private val shouldFocusSearchResults: Boolean
        get() =
            selectedShop == null &&
                query.isNotBlank() &&
                searchResultShops.isNotEmpty() &&
                !isSearchResultFocusConsumed

    private val filteredShops: RamenShops
        get() =
            when (personalizationView) {
                MapPersonalization.ALL -> shops.filterNotHidden(hiddenShopIds)
                MapPersonalization.BOOKMARKED -> shops.filterByShopIds(bookmarkedShopIds)
                MapPersonalization.HIDDEN -> shops.filterByShopIds(hiddenShopIds)
            }.filterByCategory(filters)

    private val filteredSearchResults: RamenShops
        get() =
            when (personalizationView) {
                MapPersonalization.ALL -> searchResults
                MapPersonalization.BOOKMARKED -> searchResults.filterByShopIds(bookmarkedShopIds)
                MapPersonalization.HIDDEN -> searchResults.filterByShopIds(hiddenShopIds)
            }.filterByCategory(filters)

    private val displaySearchResults: RamenShops
        get() = filteredSearchResults.markHidden(hiddenShopIds)

    private fun RamenShops.markHidden(hiddenShopIds: Set<String>): RamenShops {
        if (hiddenShopIds.isEmpty()) return this
        return RamenShops(
            mapValues { (shopId, shop) ->
                if (shopId in hiddenShopIds) {
                    shop.copy(isVisible = false)
                } else {
                    shop
                }
            },
        )
    }
}
