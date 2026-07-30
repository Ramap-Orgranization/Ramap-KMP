package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PersonalizationResponse(
    @SerialName("shop_id")
    val shopId: String,
)
