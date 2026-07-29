package com.peto.ramap.domain.model.place

import com.peto.ramap.domain.model.shop.Location

/**
 * 외부 장소 검색으로 찾은 하나의 장소를 나타낸다.
 *
 * @property name 사용자에게 표시할 검색 제공자의 장소 이름
 * @property address 사용자에게 표시할 검색 제공자의 주소
 * @property location 해당 장소로 지도를 이동할 때 사용하는 좌표
 * @property kind 검색 서버가 검증한 결과 종류. 구버전 응답은 [PlaceSearchResultKind.UNCLASSIFIED]이다.
 * @property shopId 등록 라멘 매장 결과일 때 서버가 대조한 Ramap 매장 ID
 */
data class PlaceSearchResult(
    val name: String,
    val address: String,
    val location: Location,
    val kind: PlaceSearchResultKind = PlaceSearchResultKind.UNCLASSIFIED,
    val shopId: String? = null,
)
