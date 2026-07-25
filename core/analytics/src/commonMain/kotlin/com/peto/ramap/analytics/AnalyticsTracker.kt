package com.peto.ramap.analytics

/**
 * 유저 액션과 화면 전환을 Analytics 플랫폼에 기록하는 트래커.
 *
 * Android에서는 Firebase Analytics, iOS에서는 Firebase iOS SDK 구현체가 주입된다.
 */
interface AnalyticsTracker {
    /**
     * 커스텀 이벤트를 로깅한다.
     *
     * @param event 타입
     */
    fun logEvent(event: AnalyticsEvent)

    /**
     * 화면 조회 이벤트를 로깅한다.
     *
     * @param screenName 화면 식별자. [analyticsScreenName] 매핑 사용.
     * @param params 추가 파라미터.
     */
    fun logScreenView(
        screenName: String,
        params: Map<String, Any> = emptyMap(),
    )

    /**
     * 사용자 속성을 설정한다.
     *
     * @param key 속성 키. [AnalyticsUserProperties] 상수 사용.
     * @param value 속성 값.
     */
    fun setUserProperty(
        key: String,
        value: String,
    )
}
