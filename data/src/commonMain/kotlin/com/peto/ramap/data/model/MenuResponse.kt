package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MenuResponse(
    val id: String,
    @SerialName("section_id") val sectionId: String,
    val name: String,
    @SerialName("price_krw") val priceKrw: Int? = null,
    @SerialName("price_text") val priceText: String? = null,
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("display_order") val displayOrder: Int,
    @SerialName("is_featured") val isRepresentative: Boolean = false,
)
