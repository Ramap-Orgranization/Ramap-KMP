package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MenuSectionResponse(
    val id: String,
    @SerialName("shop_id")
    val shopId: String,
    val title: String,
    val description: String? = null,
    @SerialName("display_order")
    val displayOrder: Int,
)
