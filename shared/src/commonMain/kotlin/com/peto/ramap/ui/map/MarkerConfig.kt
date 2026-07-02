package com.peto.ramap.ui.map

/**
 * 지도 마커 렌더링에 사용하는 공통 UI 설정값.
 *
 * 일반 매장 마커와 클러스터 마커의 SDK style id, 아이콘 크기, 라벨 텍스트 스타일처럼
 * 플랫폼별 지도 렌더러가 공유해야 하는 값만 이곳에서 관리한다.
 */
internal object MarkerConfig {
    object Single {
        const val STYLE_ID = "ramen-shop-marker-style"
        const val WIDTH = 24
        const val HEIGHT = 30
        const val LABEL_TEXT_SIZE = 28
        const val LABEL_TEXT_COLOR = 0xFF333333.toInt()
        const val LABEL_STROKE_WIDTH = 4
    }

    object Cluster {
        const val STYLE_ID = "ramen-shop-cluster-style"
        const val SIZE = 70
        const val TEXT_SIZE = 22
        const val TEXT_COLOR = 0xFFFFFFFF.toInt()
    }
}
