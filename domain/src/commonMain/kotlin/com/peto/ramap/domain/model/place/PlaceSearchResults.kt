package com.peto.ramap.domain.model.place

import com.peto.ramap.domain.model.shop.Location

/**
 * 장소 검색 결과의 순서와 컬렉션 행위를 캡슐화한다.
 *
 * 전달받은 결과의 순서와 중복을 보존하며, 비어 있음·개수·단일 원소 조회와
 * 인덱스 접근 및 순회는 일반 [List]와 동일하게 동작한다.
 */
data class PlaceSearchResults(
    private val results: List<PlaceSearchResult>,
) : List<PlaceSearchResult> by results {
    /**
     * [center]와 가까운 장소부터 정렬한 새 검색 결과를 반환한다.
     *
     * 거리가 같은 장소 사이의 기존 순서는 유지하며 현재 결과의 순서는 변경하지 않는다.
     */
    fun nearestFirstTo(center: Location): PlaceSearchResults =
        PlaceSearchResults(
            results.sortedBy { place -> center.distanceMetersTo(place.location) },
        )
}
