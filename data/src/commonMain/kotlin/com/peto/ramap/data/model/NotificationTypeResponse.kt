package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NotificationTypeResponse(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("display_order") val displayOrder: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("is_visible") val isVisible: Boolean,
)
