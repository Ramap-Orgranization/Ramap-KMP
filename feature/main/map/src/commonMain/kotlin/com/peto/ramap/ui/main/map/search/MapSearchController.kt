package com.peto.ramap.ui.main.map.search

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.PlaceSearchRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * 지도 검색 요청의 실행 순서, 장소 검색 fallback과 최신 요청 반영을 조정한다.
 *
 * Ramap 등록 매장을 먼저 검색하고 결과가 없을 때만 네이버 장소 검색 api를 수행한다.
 * 호출 코루틴에서 검색을 직접 실행하며 취소된 요청의 결과가 화면 상태에 반영되지 않도록
 * 각 저장소 호출 직후 코루틴 활성 상태를 확인한다.
 *
 * @property ramenShopRepository Ramap에 등록된 라멘 매장을 검색하는 저장소
 * @property placeSearchRepository 등록 매장 결과가 없을 때 일반 장소를 검색하는 저장소
 */
class MapSearchController(
    private val ramenShopRepository: RamenShopRepository,
    private val placeSearchRepository: PlaceSearchRepository,
) {
    /**
     * [query]에 대한 새로운 지도 검색을 시작한다.
     *
     * Ramap 등록 매장을 검색하고 결과가 없으면 [center]를 기준으로 일반 장소를 검색한다.
     *
     * 정규화된 검색어가 비어 있으면 저장소를 호출하지 않고 [MapSearchResult.Cleared]를 전달한다.
     *
     * @param query 검색에 사용할 정규화된 검색어
     * @param center 장소 검색 결과의 거리 기준이 되는 현재 지도 중심
     * @return 검색 결과
     */
    suspend fun search(
        query: SearchQuery,
        center: Location,
    ): MapSearchResult {
        if (query.value.isBlank()) return MapSearchResult.Cleared

        return resolveRamapShopSearch(query, center)
    }

    /**
     * Ramap에 등록된 라멘 매장을 검색한다.
     *
     * 검색 결과가 있으면 즉시 반환하고, 결과가 없으면 [resolvePlaceFallback]으로 장소 검색을 이어간다.
     *
     * @return 검색 결과
     */
    private suspend fun resolveRamapShopSearch(
        query: SearchQuery,
        center: Location,
    ): MapSearchResult {
        val shopResult = ramenShopRepository.searchRamenShops(query, SEARCH_RESULT_LIMIT)
        currentCoroutineContext().ensureActive()

        return when (shopResult) {
            is RamapResult.Success -> {
                if (shopResult.data.isNotEmpty()) {
                    MapSearchResult.Loaded(
                        query = query,
                        shops = shopResult.data,
                        places = PlaceSearchResults(emptyList()),
                    )
                } else {
                    resolvePlaceFallback(query, center, shopResult.data)
                }
            }

            is RamapResult.Error -> MapSearchResult.Failed(shopResult.error)
        }
    }

    /**
     * 등록 매장 검색 결과가 없을 때 네이버 장소 검색 api를 검색한다.
     *
     * @param query 장소 검색에 사용할 정규화된 검색어
     * @param center 검색 결과의 거리 기준이 되는 현재 지도 중심
     * @param shops 앞선 등록 매장 검색에서 반환된 빈 매장 목록
     * @return 검색 결과
     */
    private suspend fun resolvePlaceFallback(
        query: SearchQuery,
        center: Location,
        shops: RamenShops,
    ): MapSearchResult {
        val placeResult = placeSearchRepository.search(query, center)
        currentCoroutineContext().ensureActive()

        return when (placeResult) {
            is RamapResult.Success ->
                MapSearchResult.Loaded(
                    query = query,
                    shops = shops,
                    places = placeResult.data,
                )

            is RamapResult.Error -> MapSearchResult.Failed(placeResult.error)
        }
    }

    companion object {
        /** 한 번의 Ramap 등록 매장 검색에서 요청하는 최대 결과 수. */
        const val SEARCH_RESULT_LIMIT = 50
    }
}
