package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopEventParticipantResponse(
    @SerialName("shop_id") val shopId: String? = null,
)
