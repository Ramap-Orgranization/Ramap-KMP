package com.peto.ramap.core.config

/**
 * 지도 마커 렌더링에 사용하는 공통 UI 설정값.
 *
 * 일반 매장 마커의 아이콘 크기처럼 플랫폼별 지도 렌더러가 공유하는 값만 관리한다.
 */
internal object MarkerConfig {
    object Single {
        const val STYLE_ID = "ramen-shop-marker-style"
        const val HIDDEN_STYLE_ID = "hidden-ramen-shop-marker-style"
        const val WIDTH = 24
        const val HEIGHT = 30
        const val LABEL_TEXT_SIZE = 25
        const val LABEL_TEXT_COLOR = 0xFF333333.toInt()
        const val LABEL_STROKE_WIDTH = 4
    }
}
