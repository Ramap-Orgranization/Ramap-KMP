package com.peto.ramap.ui.map

import com.kakao.vectormap.KakaoMap
import com.peto.ramap.domain.model.MapBounds

/**
 * 현재 KakaoMap 화면에 보이는 영역을 도메인 `MapBounds`로 변환한다.
 *
 * 지도 이동이 끝났을 때 현재 화면 영역 기준으로 매장 목록을 다시 조회하기 위해 사용한다.
 */
internal actual class MapBoundsCalculator {
    /**
     * KakaoMap은 화면 좌표를 통해 노출 좌표를 제공하므로,
     * 측정된 map view의 네 꼭짓점 좌표로 API 조회용 bounds를 계산한다.
     */
    fun currentBounds(
        kakaoMap: KakaoMap,
        width: Int,
        height: Int,
    ): MapBounds? {
        if (width <= 0 || height <= 0) return null

        val screenPoints =
            listOfNotNull(
                kakaoMap.fromScreenPoint(0, 0),
                kakaoMap.fromScreenPoint(width, 0),
                kakaoMap.fromScreenPoint(0, height),
                kakaoMap.fromScreenPoint(width, height),
            )

        if (screenPoints.size < 4) return null

        return MapBounds(
            minLat = screenPoints.minOf { it.latitude },
            maxLat = screenPoints.maxOf { it.latitude },
            minLng = screenPoints.minOf { it.longitude },
            maxLng = screenPoints.maxOf { it.longitude },
        )
    }
}
