package com.peto.ramap.analytics

/**
 * Firebase Analytics User Property 키 상수.
 *
 * [AnalyticsTracker.setUserProperty] 호출 시 키로 사용하며,
 * 로그인 상태와 개인화 매장 수를 사용자 세그먼트 분석에 활용한다.
 */
object AnalyticsUserProperties {
    const val LOGIN_STATUS = "login_status"
    const val BOOKMARKED_COUNT = "bookmarked_count"
    const val SUBSCRIBED_COUNT = "subscribed_count"
    const val HIDDEN_COUNT = "hidden_count"
}
