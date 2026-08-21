package com.peto.ramap.analytics

enum class AnalyticsSource(
    val value: String,
) {
    MAP("map"),
    RANKING("ranking"),
    BOOKMARKED_SHOPS("bookmarked_shops"),
    HIDDEN_SHOPS("hidden_shops"),
    SUBSCRIBED_SHOPS("subscribed_shops"),
    IMPORTATION("importation"),
    NOTIFICATION_SETTINGS("notification_settings"),
    EVENT_DETAIL("event_detail"),
    ACCOUNT("account"),
    MARKER("marker"),
    SEARCH_RESULT("search_result"),
    RECENTLY_VIEWED("recently_viewed"),
    OPERATING_NOTICE("operating_notice"),
}
