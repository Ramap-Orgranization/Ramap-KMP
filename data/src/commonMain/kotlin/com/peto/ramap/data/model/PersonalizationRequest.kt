package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalizationRequest(
    @SerialName("shop_id")
    val shopId: String,
)
