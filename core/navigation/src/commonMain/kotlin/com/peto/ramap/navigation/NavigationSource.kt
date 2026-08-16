package com.peto.ramap.navigation

import kotlinx.serialization.Serializable

@Serializable
enum class NavigationSource(
    val value: String,
) {
    RANKING("ranking"),
    HIDDEN_SHOPS("hidden_shops"),
    SUBSCRIBED_SHOPS("subscribed_shops"),
    BOOKMARKED_SHOPS("bookmarked_shops"),
    EVENT_DETAIL("event_detail"),
    SHARED_LINK("shared_link"),
}
