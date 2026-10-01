package com.peto.ramap.debug.admin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdminParticipant(
    val name: String,
    @SerialName("instagram_url") val instagramUrl: String? = null,
)
