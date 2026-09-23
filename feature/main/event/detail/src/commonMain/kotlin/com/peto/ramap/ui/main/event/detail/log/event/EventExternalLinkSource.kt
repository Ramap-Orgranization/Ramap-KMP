package com.peto.ramap.ui.main.event.detail.log.event

internal enum class EventExternalLinkSource(
    val value: String,
) {
    COLLABORATOR_INSTAGRAM("collaborator_instagram"),
    VENUE_INSTAGRAM("venue_instagram"),
    VENUE_NAVER_MAP("venue_naver_map"),
    VENUE_KAKAO_MAP("venue_kakao_map"),
    WAITING("waiting"),
    EVENT_SOURCE("event_source"),
}
