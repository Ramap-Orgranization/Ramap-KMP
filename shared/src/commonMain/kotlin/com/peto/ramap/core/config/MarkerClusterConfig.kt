package com.peto.ramap.core.config

/**
 * 클러스터 계산과 표시 정책에 사용하는 설정값.
 *
 * 렌더링 전용 값은 각 플랫폼 UI 계층에 두고, 몇 개부터 묶을지, 어느 거리까지 묶어둘지,
 * 줌 변화에 언제 반응할지처럼 클러스터 동작을 결정하는 값만 이곳에서 관리한다.
 */
internal object MarkerClusterConfig {
    /**
     * 클러스터 마커로 묶기 시작할 최소 매장 수.
     *
     * 같은 cell 안에 있는 매장 수가 이 값보다 작으면 단일 매장 마커로 표시하고
     * 이 값 이상이면 클러스터 마커로 표시한다.
     */
    const val MIN_SHOP_COUNT = 3

    /**
     * 같은 클러스터로 묶어둘 화면상 거리 기준.
     *
     * 값이 클수록 더 멀리 떨어진 매장도 같은 cell에 들어가므로 클러스터가 늦게 해제되고,
     * 값이 작을수록 가까운 매장만 묶여 줌인할 때 클러스터가 더 빨리 단일 마커로 풀린다.
     */
    const val RELEASE_DISTANCE_PX = 120.0

    /**
     * 클러스터를 다시 계산할 줌 변화 비율.
     *
     * 지도 이동만으로는 [com.peto.ramap.domain.model.MapBounds] 중심만 바뀌므로 클러스터 기준 bounds를 유지하고,
     * 화면 위경도 span이 이 비율 이상 달라질 때만 줌 변경으로 보고 클러스터를 다시 계산한다.
     * 값이 작을수록 작은 줌 변화에도 클러스터가 갱신되고, 값이 클수록 더 큰 줌 변화가 있어야 갱신된다.
     */
    const val ZOOM_SHIFT_RATIO = 0.01

    /**
     * 클러스터 마커에 그대로 표시할 최대 매장 수.
     *
     * 이 값보다 큰 클러스터는 [MAX_COUNT_TEXT]로 축약해 표시한다.
     */
    const val MAX_COUNT = 99

    const val MAX_COUNT_TEXT = "99+"
    const val MAX_COUNT_STYLE_BUCKET = "99-plus"
    const val ID_SEPARATOR = "-"

    fun countText(count: Int): String =
        if (count > MAX_COUNT) {
            MAX_COUNT_TEXT
        } else {
            count.toString()
        }

    fun countStyleBucket(count: Int): String =
        if (count > MAX_COUNT) {
            MAX_COUNT_STYLE_BUCKET
        } else {
            count.toString()
        }
}
