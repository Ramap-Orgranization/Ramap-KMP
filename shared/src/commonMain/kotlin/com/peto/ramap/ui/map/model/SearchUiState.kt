package com.peto.ramap.ui.map.model

import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.SearchQuery

data class SearchUiState(
    /**
     * 검색창에 입력된 원본 검색어.
     */
    val input: String = "",
    /**
     * 현재 검색 결과 매장 목록.
     */
    val results: RamenShops = RamenShops(emptyMap()),
    /**
     * [results]가 어떤 정규화된 검색어에 대한 결과인지 나타내는 값.
     */
    private val loadedQuery: SearchQuery? = null,
    /**
     * 사용자가 현재 검색 결과 바텀시트를 닫았는지 여부.
     */
    val isResultsDismissed: Boolean = false,
    /**
     * 현재 검색 결과에 대한 지도 카메라 포커스가 이미 처리되었는지 여부.
     */
    val isResultFocusConsumed: Boolean = false,
) {
    val hasLoadedResultsForInput: Boolean
        get() {
            val normalizedInput = SearchQuery(input).normalizeShopSearchQuery()
            return normalizedInput.value.isNotBlank() && loadedQuery == normalizedInput
        }

    fun hasLoadedResultsFor(query: SearchQuery): Boolean = query.value.isNotBlank() && loadedQuery == query

    fun updateInput(input: String): SearchUiState =
        copy(
            input = input,
            isResultsDismissed = false,
            isResultFocusConsumed = false,
        )

    fun dismissResults(): SearchUiState = copy(isResultsDismissed = true)

    fun showResults(): SearchUiState = copy(isResultsDismissed = false)

    fun consumeResultFocus(shouldConsume: Boolean): SearchUiState = copy(isResultFocusConsumed = isResultFocusConsumed || shouldConsume)

    fun clearResults(): SearchUiState =
        copy(
            results = RamenShops(emptyMap()),
            loadedQuery = null,
            isResultsDismissed = false,
            isResultFocusConsumed = false,
        )

    fun updateResults(
        query: SearchQuery,
        results: RamenShops,
    ): SearchUiState =
        copy(
            results = results,
            loadedQuery = query,
            isResultFocusConsumed = false,
        )

    companion object {
        fun loaded(
            input: String,
            results: RamenShops,
            isResultFocusConsumed: Boolean = false,
        ): SearchUiState {
            val loadedQuery = SearchQuery(input).normalizeShopSearchQuery()
            return SearchUiState(
                input = input,
                results = results,
                loadedQuery = loadedQuery,
                isResultFocusConsumed = isResultFocusConsumed,
            )
        }
    }
}
