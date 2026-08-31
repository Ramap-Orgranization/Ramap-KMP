package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopEventParticipantResponse(
    @SerialName("event_id") val eventId: String,
    @SerialName("shop_id") val shopId: String? = null,
)
