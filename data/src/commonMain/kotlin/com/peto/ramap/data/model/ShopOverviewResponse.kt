package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopOverviewResponse(
    val shop: RamenShopResponse,
    @SerialName("like_count")
    val likeCount: Long,
    val events: List<ShopEventResponse> = emptyList(),
    @SerialName("event_participants")
    val eventParticipants: List<ShopEventParticipantResponse> = emptyList(),
    @SerialName("operating_notice")
    val operatingNotice: OperatingNoticeResponse? = null,
)
