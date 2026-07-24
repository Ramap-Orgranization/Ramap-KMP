package com.peto.ramap.analytics

/**
 * Firebase Analytics 이벤트 파라미터 키 상수.
 *
 * [AnalyticsTracker.logEvent] 호출 시 params 맵의 키로 사용한다.
 */
object AnalyticsParams {
    const val SHOP_ID = "shop_id"
    const val SHOP_NAME = "shop_name"
    const val SOURCE = "source"
    const val CATEGORY = "category"
    const val CATEGORY_COUNT = "category_count"
    const val HAS_CATEGORY = "has_category"
    const val EVENT_ID = "event_id"
    const val EVENT_STATUS = "event_status"
    const val EVENT_TYPE = "event_type"
    const val IS_TODAY = "is_today"
    const val VENUE_SHOP_ID = "venue_shop_id"
    const val METHOD = "method"
    const val PLACE_NAME = "place_name"
    const val AREA = "area"
    const val HAS_URL = "has_url"
    const val HAS_DESCRIPTION = "has_description"
    const val WRONG_FIELD_COUNT = "wrong_field_count"
    const val ENABLED = "enabled"
}
