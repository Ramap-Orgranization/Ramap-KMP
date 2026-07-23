package com.peto.ramap.ui.main.map.search

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.place.PlaceSearchResultKind
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.PlaceSearchRepository
import com.peto.ramap.domain.repository.RamenShopRepository

/**
 * 지도 검색 요청의 실행 순서를 관리하고, 장소 검색 폴백 여부와 최신 요청 결과 반영을 조정한다.
 * 서버에 등록된 매장을 먼저 검색하고 결과가 없을 때만 Supabase `place-search` Edge Function을 호출한다.
 * Edge Function은 네이버 지역 검색·Geocoding API 결과를 Ramap DB와 대조해 지도 이동 장소와
 * 등록 라멘 매장으로 분류하며, 앱은 서버가 허용한 결과만 처리한다.
 *
 * @property ramenShopRepository 서버에 등록된 라멘 매장을 검색하는 저장소
 * @property placeSearchRepository `place-search` Edge Function이 분류·허용한 장소를 조회하는 저장소
 */
internal class MapSearchController(
    private val ramenShopRepository: RamenShopRepository,
    private val placeSearchRepository: PlaceSearchRepository,
) {
    /**
     * [query]로 새로운 지도 검색을 시작한다.
     *
     * 서버에 등록된 매장을 먼저 검색하고 결과가 없으면 [center]를 기준으로 `place-search` Edge Function에
     * 분류 결과를 요청한다. 네이버 API 호출과 Ramap DB 대조는 Edge Function에서 수행한다.
     *
     * 정규화된 검색어가 비어 있으면 저장소를 호출하지 않고 [MapSearchResult.Cleared]를 반환한다.
     *
     * @param query 검색에 사용할 정규화된 검색어
     * @param center 장소 검색 결과의 거리 기준이 되는 현재 지도 중심
     * @return 검색어가 비어 있으면 초기화 결과, 그 외에는 매장·장소 검색 결과 또는 저장소 오류
     */
    suspend fun search(
        query: SearchQuery,
        center: Location,
    ): MapSearchResult {
        if (query.value.isBlank()) return MapSearchResult.Cleared

        return resolveRamapShopSearch(query, center)
    }

    /**
     * Ramap 등록 매장을 우선 검색하고, 매장이 없을 때만 `place-search` Edge Function 조회로 넘어간다.
     *
     * @param query 등록 매장 이름 검색에 사용할 정규화된 검색어
     * @param center 이어지는 장소 검색의 거리 기준이 되는 현재 지도 중심
     * @return 등록 매장 결과, Edge Function이 허용한 장소 검색 결과 또는 매장 저장소 오류
     */
    private suspend fun resolveRamapShopSearch(
        query: SearchQuery,
        center: Location,
    ): MapSearchResult {
        val shopResult = ramenShopRepository.searchRamenShops(query, SEARCH_RESULT_LIMIT)

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
     * 등록 매장 결과가 없을 때 `place-search` Edge Function이 허용한 장소 결과를 조회한다.
     *
     * @param query 장소 검색에 사용할 정규화된 검색어
     * @param center 장소 검색의 거리 기준이 되는 현재 지도 중심
     * @param shops 앞서 진행한 등록 매장 검색에서 확인된 빈 매장 목록
     * @return Edge Function의 분류를 반영한 검색 결과 또는 장소 저장소 오류
     */
    private suspend fun resolvePlaceFallback(
        query: SearchQuery,
        center: Location,
        shops: RamenShops,
    ): MapSearchResult {
        val placeResult = placeSearchRepository.search(query, center)

        return when (placeResult) {
            is RamapResult.Success -> {
                resolveVerifiedPlaceResult(query, shops, placeResult.data)
            }

            is RamapResult.Error -> MapSearchResult.Failed(placeResult.error)
        }
    }

    /**
     * Edge Function이 분류·허용한 장소를 지도 위치와 등록 매장으로 나누어 앱 검색 결과로 변환한다.
     *
     * 종류가 분류되지 않은 장소와 매장 ID가 없는 등록 매장 결과는 노출하지 않는다.
     *
     * @param query 최종 검색 결과에 포함할 정규화된 검색어
     * @param initialShops 앞서 진행한 등록 매장 검색 결과
     * @param places Edge Function이 네이버 API 결과와 Ramap DB를 대조해 분류한 장소 검색 결과
     * @return 노출 가능한 지도 위치와 조회된 등록 매장, 혹은 등록 매장 조회 오류
     */
    private suspend fun resolveVerifiedPlaceResult(
        query: SearchQuery,
        initialShops: RamenShops,
        places: PlaceSearchResults,
    ): MapSearchResult {
        val mapLocations = selectMapLocations(places)
        val registeredShopIds = collectRegisteredShopIds(places)
        val shopsById = fetchRegisteredShops(registeredShopIds)
        val fetchedShops =
            when (shopsById) {
                is RamapResult.Success -> shopsById.data
                is RamapResult.Error -> return MapSearchResult.Failed(shopsById.error)
            }

        return buildLoadedResult(query, initialShops, fetchedShops, mapLocations)
    }

    /**
     * Edge Function이 지도 이동 대상으로 분류한 장소만 골라낸다.
     *
     * @param places Edge Function이 네이버 API 결과와 Ramap DB를 대조해 분류한 장소 검색 결과
     * @return 입력 순서를 그대로 유지한 지도 이동 장소 목록
     */
    private fun selectMapLocations(places: PlaceSearchResults): PlaceSearchResults {
        val mapLocations = mutableListOf<PlaceSearchResult>()
        for (place in places) {
            if (place.kind == PlaceSearchResultKind.MAP_LOCATION) mapLocations += place
        }
        return PlaceSearchResults(mapLocations)
    }

    /**
     * Edge Function이 등록 매장으로 분류한 장소에서 유효한 Ramap 매장 ID를 뽑아낸다.
     *
     * @param places Edge Function이 네이버 API 결과와 Ramap DB를 대조해 분류한 장소 검색 결과
     * @return 빈 값을 제외하고 중복도 제거한 등록 매장 ID 집합
     */
    private fun collectRegisteredShopIds(places: PlaceSearchResults): Set<String> {
        val registeredShopIds = linkedSetOf<String>()
        for (place in places) {
            if (place.kind == PlaceSearchResultKind.REGISTERED_SHOP) {
                place.shopId?.takeIf(String::isNotBlank)?.let(registeredShopIds::add)
            }
        }
        return registeredShopIds
    }

    /**
     * Edge Function이 Ramap DB와 대조해 반환한 매장 ID에 해당하는 매장 정보를 조회한다.
     *
     * ID가 없으면 저장소를 호출하지 않으며, 실제로 호출한 경우에는 취소 여부를 확인한다.
     *
     * @param shopIds 조회할 Ramap 매장 ID 집합
     * @return 조회된 매장 목록 또는 매장 저장소 오류
     */
    private suspend fun fetchRegisteredShops(shopIds: Set<String>): RamapResult<RamenShops> {
        if (shopIds.isEmpty()) return RamapResult.Success(RamenShops(emptyMap()))

        val result = ramenShopRepository.fetchRamenShops(shopIds)
        return result
    }

    /**
     * 기존 매장, Edge Function이 검증한 매장, 지도 위치를 하나의 성공 검색 결과로 합친다.
     *
     * @param query 검색 결과에 포함할 정규화된 검색어
     * @param initialShops 등록 매장 이름 검색으로 얻은 매장 목록
     * @param fetchedShops Edge Function이 내려준 ID로 조회한 매장 목록
     * @param mapLocations Edge Function이 지도 이동 대상으로 분류한 장소 목록
     * @return 노출 가능한 결과를 모두 담은 성공 검색 결과
     */
    private fun buildLoadedResult(
        query: SearchQuery,
        initialShops: RamenShops,
        fetchedShops: RamenShops,
        mapLocations: PlaceSearchResults,
    ): MapSearchResult.Loaded =
        MapSearchResult.Loaded(
            query = query,
            shops = RamenShops(initialShops.values + fetchedShops.values),
            places = mapLocations,
        )

    companion object {
        /** 한 번의 Ramap 등록 매장 검색에서 요청할 최대 결과 수. */
        const val SEARCH_RESULT_LIMIT = 50
    }
}
