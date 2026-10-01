package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopDetailResponse(
    val shop: RamenShopResponse,
    @SerialName("like_count")
    val likeCount: Long,
    @SerialName("waiting_system")
    val waitingSystem: LegacyShopWaitingSystemResponse? = null,
    val events: List<ShopEventResponse> = emptyList(),
    @SerialName("event_participants")
    val eventParticipants: List<ShopEventParticipantResponse> = emptyList(),
    @SerialName("operating_notice")
    val operatingNotice: OperatingNoticeResponse? = null,
    @SerialName("menu_sections")
    val menuSections: List<MenuSectionResponse> = emptyList(),
    @SerialName("menu_items")
    val menuItems: List<MenuResponse> = emptyList(),
)
