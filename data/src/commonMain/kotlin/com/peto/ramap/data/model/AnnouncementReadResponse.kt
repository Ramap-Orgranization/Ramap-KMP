package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AnnouncementReadResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("announcement_id") val announcementId: String,
)
