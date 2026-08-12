package com.peto.ramap.data.model

import com.peto.ramap.domain.model.event.ExternalParticipant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ExternalParticipantResponse(
    val name: String? = null,
    @SerialName("instagram_url") val instagramUrl: String? = null,
) {
    fun toDomain(): ExternalParticipant =
        ExternalParticipant(
            name = name,
            instagramUrl = instagramUrl,
        )
}
