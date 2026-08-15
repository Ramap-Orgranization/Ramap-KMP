package com.peto.ramap.ui.main.map.search

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.RamenShopRepository

/** 지도 검색 요청의 실행 순서를 관리한다. */
internal class MapSearchController(
    private val ramenShopRepository: RamenShopRepository,
) {
    /** 등록된 라멘 매장을 이름으로 검색한다. */
    suspend fun search(query: SearchQuery): MapSearchResult {
        if (query.value.isBlank()) return MapSearchResult.Cleared

        return when (val result = ramenShopRepository.searchRamenShops(query, SEARCH_RESULT_LIMIT)) {
            is RamapResult.Success ->
                MapSearchResult.Loaded(
                    query = query,
                    shops = result.data,
                )

            is RamapResult.Error -> MapSearchResult.Failed(result.error)
        }
    }

    companion object {
        /** 한 번의 등록 매장 검색에서 요청할 최대 결과 수. */
        const val SEARCH_RESULT_LIMIT = 50
    }
}
