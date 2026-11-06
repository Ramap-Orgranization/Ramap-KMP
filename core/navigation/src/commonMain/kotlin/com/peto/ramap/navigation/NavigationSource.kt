package com.peto.ramap.navigation

import kotlinx.serialization.Serializable

@Serializable
enum class NavigationSource(
    val value: String,
) {
    RANKING("ranking"),

    // 이전 버전에서 저장한 지도 경로를 복원할 때 사용한다.
    HIDDEN_SHOPS("hidden_shops"),
    SUBSCRIBED_SHOPS("subscribed_shops"),
    BOOKMARKED_SHOPS("bookmarked_shops"),
    EVENT_DETAIL("event_detail"),
    SHARED_LINK("shared_link"),
    OPERATING_NOTICE("operating_notice"),
}
