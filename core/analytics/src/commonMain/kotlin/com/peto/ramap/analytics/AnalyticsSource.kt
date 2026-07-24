package com.peto.ramap.analytics

/**
 * 유저 액션의 진입 경로를 나타내는 source 파라미터 값 상수.
 *
 * [AnalyticsParams.SOURCE] 키와 함께 사용하여 동일 액션의 발생 지점을 구분한다.
 */
object AnalyticsSource {
    const val MAP = "map"
    const val RANKING = "ranking"
    const val ACCOUNT = "account"
    const val MARKER = "marker"
    const val RANKING_BOOKMARK = "ranking_bookmark"
}
