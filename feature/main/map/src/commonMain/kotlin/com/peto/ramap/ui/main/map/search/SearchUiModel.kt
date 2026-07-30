package com.peto.ramap.ui.main.map.search

import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery

/**
 * 지도 검색 화면에서 검색어, 검색 결과, 결과 바텀시트와 카메라 포커스 요청을 관리하는 UI 모델.
 *
 * 등록된 라멘 매장 검색 결과와 외부 장소 검색 결과를 함께 보관하고, 각 결과가 현재 입력과
 * 일치하는지 및 사용자에게 이미 표시되거나 소비되었는지를 표현한다.
 */
data class SearchUiModel(
    /** 검색창에 입력된 정규화 전 원본 검색어. */
    val input: String = "",
    /** 등록된 라멘 매장 중 [loadedQuery]에 대응하는 검색 결과. */
    val results: RamenShops = RamenShops(emptyMap()),
    /** [results]를 조회할 때 사용한 정규화된 검색어. */
    private val loadedQuery: SearchQuery? = null,
    /** 사용자가 현재 검색 결과 바텀시트를 닫았는지 여부. */
    val isResultsDismissed: Boolean = false,
    /** 현재 매장 검색 결과에 대한 지도 카메라 포커스가 처리되었는지 여부. */
    val isResultFocusConsumed: Boolean = false,
    /**
     * 매장 검색 결과에 대한 카메라 포커스 요청을 식별하는 일련번호.
     *
     * 같은 결과를 다시 포커스해야 할 때도 새로운 요청으로 인식할 수 있도록 검색 입력이 갱신될 때마다 증가한다.
     */
    val focusRequestKey: Long = 0,
    /** 등록 매장 검색 결과가 없을 때 조회한 외부 장소 검색 결과. */
    val placeResults: PlaceSearchResults = PlaceSearchResults(emptyList()),
    /** 사용자가 선택한 외부 장소로 지도 카메라를 이동할 위치. */
    val placeFocusLocation: Location? = null,
    /**
     * 외부 장소에 대한 카메라 포커스 요청을 식별하는 일련번호.
     *
     * 동일한 장소를 다시 선택해도 카메라 이동이 새 요청으로 처리되도록 선택할 때마다 증가한다.
     */
    val placeFocusRequestKey: Long = 0,
) {
    /**
     * [results]가 현재 [input]을 정규화한 검색어에 대응하는지 여부.
     *
     * 새로운 검색 결과가 도착하기 전 이전 검색 결과를 현재 입력의 결과로 표시하지 않도록 한다.
     */
    val hasLoadedResultsForInput: Boolean
        get() {
            val normalizedInput = SearchQuery(input).normalizeShopSearchQuery()
            return normalizedInput.value.isNotBlank() && loadedQuery == normalizedInput
        }

    /**
     * [results]가 주어진 정규화 검색어 [query]에 대응하는지 확인한다.
     *
     * @param query 결과 보유 여부를 확인할 정규화된 검색어
     */
    fun hasLoadedResultsFor(query: SearchQuery): Boolean = query.value.isNotBlank() && loadedQuery == query

    /**
     * 검색창 입력을 [input]으로 변경하고 새로운 검색 결과를 표시할 준비를 한다.
     *
     * 기존 장소 검색 결과와 장소 포커스 요청을 제거하고, 닫힘 및 매장 포커스 소비 상태를
     * 초기화한다.
     *
     * @param input 검색창에 입력된 정규화 전 원본 검색어
     */
    fun updateInput(input: String): SearchUiModel =
        copy(
            input = input,
            isResultsDismissed = false,
            isResultFocusConsumed = false,
            focusRequestKey = focusRequestKey + 1,
            placeResults = PlaceSearchResults(emptyList()),
            placeFocusLocation = null,
        )

    /** 현재 검색 결과 바텀시트를 사용자가 닫은 상태로 변경한다. */
    fun dismissResults(): SearchUiModel = copy(isResultsDismissed = true)

    /** 현재 검색 결과 바텀시트를 다시 표시할 수 있는 상태로 변경한다. */
    fun showResults(): SearchUiModel = copy(isResultsDismissed = false)

    /**
     * 현재 매장 검색 결과의 카메라 포커스 소비 여부를 갱신한다.
     *
     * 한 번 소비된 포커스는 [shouldConsume]이 `false`여도 다시 미소비 상태로 돌아가지 않는다.
     *
     * @param shouldConsume 이번 상태 전이에서 포커스를 소비해야 하는지 여부
     */
    fun consumeResultFocus(shouldConsume: Boolean): SearchUiModel = copy(isResultFocusConsumed = isResultFocusConsumed || shouldConsume)

    /**
     * 매장 및 외부 장소 검색 결과와 결과 표시 상태를 초기화한다.
     *
     * 검색창의 [input]과 포커스 요청 일련번호는 유지한다.
     */
    fun clearResults(): SearchUiModel =
        copy(
            results = RamenShops(emptyMap()),
            loadedQuery = null,
            isResultsDismissed = false,
            isResultFocusConsumed = false,
            placeResults = PlaceSearchResults(emptyList()),
            placeFocusLocation = null,
        )

    /** 검색어와 매장·장소 검색 결과를 모두 초기화한다. */
    fun reset(): SearchUiModel = SearchUiModel()

    /**
     * 정규화 검색어 [query]에 대한 등록 매장 검색 결과를 저장한다.
     *
     * 이전 외부 장소 결과와 장소 포커스 요청을 제거하고, 새 매장 결과에 대한 카메라 포커스를
     * 수행할 수 있도록 소비 상태를 초기화한다.
     *
     * @param query [results]를 조회할 때 사용한 정규화 검색어
     * @param results 등록 매장 검색 결과
     */
    fun updateResults(
        query: SearchQuery,
        results: RamenShops,
    ): SearchUiModel =
        copy(
            results = results,
            loadedQuery = query,
            isResultFocusConsumed = false,
            placeResults = PlaceSearchResults(emptyList()),
            placeFocusLocation = null,
        )

    /**
     * 외부 장소 검색 결과를 저장하고 결과 바텀시트를 표시할 수 있는 상태로 변경한다.
     *
     * @param results 외부 장소 검색 결과
     */
    fun updatePlaceResults(results: PlaceSearchResults): SearchUiModel =
        copy(
            placeResults = results,
            isResultsDismissed = false,
            placeFocusLocation = null,
        )

    /**
     * 외부 장소 [place]를 선택하고 해당 위치로 카메라를 이동하는 요청을 생성한다.
     *
     * 장소 결과와 결과 바텀시트를 닫고 [placeFocusRequestKey]를 증가시켜 동일한 장소를 다시
     * 선택한 경우에도 새로운 포커스 요청으로 처리한다.
     *
     * @param place 사용자가 선택한 외부 장소
     */
    fun selectPlace(place: PlaceSearchResult): SearchUiModel =
        copy(
            placeResults = PlaceSearchResults(emptyList()),
            isResultsDismissed = true,
            placeFocusLocation = place.location,
            placeFocusRequestKey = placeFocusRequestKey + 1,
        )
}
