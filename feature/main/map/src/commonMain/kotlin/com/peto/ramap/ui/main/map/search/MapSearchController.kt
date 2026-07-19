package com.peto.ramap.ui.main.map.search

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.PlaceSearchRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 지도 검색 요청의 실행 순서, 장소 검색 fallback과 최신 요청 반영을 조정한다.
 *
 * Ramap 등록 매장을 먼저 검색하고 결과가 없을 때만 네이버 장소 검색 api를 수행한다.
 * 새로운 검색이 시작되면 이전 작업을 취소하며 취소에 협조하지 않은 이전 요청이 완료되더라도
 * 요청 ID를 비교해 오래된 결과가 화면 상태에 반영되지 않도록 한다.
 *
 * @property ramenShopRepository Ramap에 등록된 라멘 매장을 검색하는 저장소
 * @property placeSearchRepository 등록 매장 결과가 없을 때 일반 장소를 검색하는 저장소
 * @property coroutineScope 검색 작업을 실행하고 취소 수명주기를 공유하는 코루틴 스코프
 */
class MapSearchController(
    private val ramenShopRepository: RamenShopRepository,
    private val placeSearchRepository: PlaceSearchRepository,
    private val coroutineScope: CoroutineScope,
) {
    /** 현재 실행 중인 검색 작업. */
    private var job: Job? = null

    /** 새로운 검색이나 명시적 취소를 구분하는 ID. */
    private var requestId = 0L

    /**
     * [query]에 대한 새로운 지도 검색을 시작한다.
     *
     * 진행 중인 검색을 취소한 뒤 Ramap 등록 매장을 검색하고, 등록 매장이 없으면 [center]를
     * 기준으로 일반 장소를 검색한다.
     *
     * 정규화된 검색어가 비어 있으면 저장소를 호출하지 않고 [MapSearchResult.Cleared]를 전달한다.
     *
     * @param query 검색에 사용할 정규화된 검색어
     * @param center 장소 검색 결과의 거리 기준이 되는 현재 지도 중심
     * @param onResult 최신 검색 요청의 결과를 전달받는 콜백
     */
    fun search(
        query: SearchQuery,
        center: Location,
        onResult: suspend (MapSearchResult) -> Unit,
    ) {
        cancel()
        val currentRequestId = requestId
        job =
            coroutineScope.launch {
                val result = resolveSearch(query, center, currentRequestId) ?: return@launch
                deliverIfCurrent(currentRequestId, result, onResult)
            }
    }

    /**
     * 검색어가 비어 있는지 확인하고 실제 검색 단계로 진입한다.
     *
     * @return 검색 결과, 또는 처리 중 요청이 오래된 요청이 되면 `null`
     */
    private suspend fun resolveSearch(
        query: SearchQuery,
        center: Location,
        currentRequestId: Long,
    ): MapSearchResult? {
        if (query.value.isBlank()) return MapSearchResult.Cleared

        return resolveRamapShopSearch(query, center, currentRequestId)
    }

    /**
     * Ramap에 등록된 라멘 매장을 검색한다.
     *
     * 검색 결과가 있으면 즉시 반환하고, 결과가 없으면 [resolvePlaceFallback]으로 장소 검색을 이어간다.
     *
     * @return 검색 결과, 또는 처리 중 요청이 오래된 요청이 되면 `null`
     */
    private suspend fun resolveRamapShopSearch(
        query: SearchQuery,
        center: Location,
        currentRequestId: Long,
    ): MapSearchResult? {
        val shopResult = ramenShopRepository.searchRamenShops(query, SEARCH_RESULT_LIMIT)
        if (!isCurrentRequest(currentRequestId)) return null

        return when (shopResult) {
            is RamapResult.Success -> {
                if (shopResult.data.isNotEmpty()) {
                    MapSearchResult.Loaded(
                        query = query,
                        shops = shopResult.data,
                        places = PlaceSearchResults(emptyList()),
                    )
                } else {
                    resolvePlaceFallback(query, center, shopResult.data, currentRequestId)
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
     * @param currentRequestId 현재 검색 작업이 시작될 때 발급된 요청 ID
     * @return 검색 결과, 또는 처리 중 요청이 오래된 요청이 되면 `null`
     */
    private suspend fun resolvePlaceFallback(
        query: SearchQuery,
        center: Location,
        shops: RamenShops,
        currentRequestId: Long,
    ): MapSearchResult? {
        val placeResult = placeSearchRepository.search(query, center)
        if (!isCurrentRequest(currentRequestId)) return null

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

    /**
     * [currentRequestId]가 최신 요청일 때만 [result]를 전달한다.
     *
     * 저장소 호출 이후 결과를 만든 시점과 콜백을 실행하는 시점 사이에 새 검색이 시작되는 경우도
     * 오래된 결과가 반영되지 않도록 마지막으로 요청 ID를 확인한다.
     */
    private suspend fun deliverIfCurrent(
        currentRequestId: Long,
        result: MapSearchResult,
        onResult: suspend (MapSearchResult) -> Unit,
    ) {
        if (isCurrentRequest(currentRequestId)) {
            onResult(result)
        }
    }

    /** [currentRequestId]가 현재 최신 검색 요청의 ID인지 확인한다. */
    private fun isCurrentRequest(currentRequestId: Long): Boolean = currentRequestId == requestId

    /**
     * 현재 검색 작업을 취소하고 진행 중인 요청의 결과를 무효화한다.
     *
     * 코루틴 취소에 협조하지 않는 저장소 작업이 늦게 완료되더라도 결과를 버릴 수 있도록
     * [requestId]를 함께 증가시킨다.
     */
    fun cancel() {
        requestId += 1
        job?.cancel()
        job = null
    }

    companion object {
        /** 한 번의 Ramap 등록 매장 검색에서 요청하는 최대 결과 수. */
        const val SEARCH_RESULT_LIMIT = 50
    }
}
