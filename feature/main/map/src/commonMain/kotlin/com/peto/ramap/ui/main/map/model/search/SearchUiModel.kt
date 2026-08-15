package com.peto.ramap.ui.main.map.model.search

import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery

/** 지도 검색 화면에서 검색어와 등록 매장 검색 결과를 관리하는 UI 모델. */
data class SearchUiModel(
    /** 검색창에 입력된 정규화 전 원본 검색어. */
    val input: String = "",
    /** [loadedQuery]에 대응하는 등록 라멘 매장 검색 결과. */
    val results: RamenShops = RamenShops(emptyMap()),
    /** [results]를 조회할 때 사용한 정규화된 검색어. */
    private val loadedQuery: SearchQuery? = null,
    /** 사용자가 현재 검색 결과 바텀시트를 닫았는지 여부. */
    val isResultsDismissed: Boolean = false,
    /** 현재 매장 검색 결과에 대한 지도 카메라 포커스가 처리되었는지 여부. */
    val isResultFocusConsumed: Boolean = false,
    /** 매장 검색 결과에 대한 카메라 포커스 요청을 식별하는 일련번호. */
    val focusRequestKey: Long = 0,
) {
    /** [results]가 현재 입력을 정규화한 검색어에 대응하는지 여부. */
    val hasLoadedResultsForInput: Boolean
        get() {
            val normalizedInput = SearchQuery(input).normalizeShopSearchQuery()
            return normalizedInput.value.isNotBlank() && loadedQuery == normalizedInput
        }

    fun hasLoadedResultsFor(query: SearchQuery): Boolean = query.value.isNotBlank() && loadedQuery == query

    /** 검색창 입력을 [input]으로 변경하고 새 결과를 표시할 준비를 한다. */
    fun updateInput(input: String): SearchUiModel =
        copy(
            input = input,
            isResultsDismissed = false,
            isResultFocusConsumed = false,
            focusRequestKey = focusRequestKey + 1,
        )

    fun dismissResults(): SearchUiModel = copy(isResultsDismissed = true)

    /** 현재 매장 검색 결과의 카메라 포커스 소비 여부를 갱신한다. */
    fun consumeResultFocus(shouldConsume: Boolean): SearchUiModel = copy(isResultFocusConsumed = isResultFocusConsumed || shouldConsume)

    /** 검색 결과와 결과 표시 상태를 초기화한다. */
    fun clearResults(): SearchUiModel =
        copy(
            results = RamenShops(emptyMap()),
            loadedQuery = null,
            isResultsDismissed = false,
            isResultFocusConsumed = false,
        )

    fun reset(): SearchUiModel = SearchUiModel()

    /** 정규화 검색어 [query]에 대한 등록 매장 검색 결과를 저장한다. */
    fun updateResults(
        query: SearchQuery,
        results: RamenShops,
    ): SearchUiModel =
        copy(
            results = results,
            loadedQuery = query,
            isResultFocusConsumed = false,
        )
}
