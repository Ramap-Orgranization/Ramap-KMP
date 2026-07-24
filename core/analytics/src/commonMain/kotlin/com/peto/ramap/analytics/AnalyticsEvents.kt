package com.peto.ramap.analytics

/** Firebase Analytics 이벤트명 상수. */
object AnalyticsEvents {
    // ── 매장 발견 (Map) ──
    const val SHOP_SELECT = "shop_select"
    const val SHOP_DETAIL_VIEW = "shop_detail_view"

    // ── 검색 ──
    const val SEARCH_PLACE_SELECT = "search_place_select"

    // ── 필터 ──
    const val CATEGORY_FILTER_TOGGLE = "category_filter_toggle"
    const val CATEGORY_FILTER_ALL = "category_filter_all"
    const val AREA_FILTER_SELECT = "area_filter_select"
    const val BOOKMARKED_VIEW_TOGGLE = "bookmarked_view_toggle"

    // ── 매장 개인화 ──
    const val BOOKMARK_TOGGLE = "bookmark_toggle"
    const val SHOP_NOTIFICATION_TOGGLE = "shop_notification_toggle"
    const val SHOP_HIDE_TOGGLE = "shop_hide_toggle"
    const val SHOP_REPORT_SUBMIT = "shop_report_submit"

    // ── 랭킹 ──
    const val RANKING_PAGE_LOAD = "ranking_page_load"

    // ── 이벤트 ──
    const val EVENT_DETAIL_VIEW = "event_detail_view"
    const val EVENT_NOTIFICATION_TOGGLE = "event_notification_toggle"
    const val EVENT_UNAVAILABLE = "event_unavailable"
    const val EVENT_SELECT = "event_select"
    const val EVENT_SHOP_SELECT = "event_shop_select"
    const val EVENT_EXTERNAL_LINK_SELECT = "event_external_link_select"

    // ── 인증 ──
    const val LOGIN_START = "login_start"
    const val LOGIN_SUCCESS = "login_success"
    const val LOGIN_FAILURE = "login_failure"

    // ── 알림 ──
    const val NOTIFICATION_OPEN = "notification_open"

    // ── 에러 & 재시도 ──
    const val VIEWPORT_LOAD_ERROR = "viewport_load_error"
}
