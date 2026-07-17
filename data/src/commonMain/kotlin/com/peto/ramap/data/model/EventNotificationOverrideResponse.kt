package com.peto.ramap.data.model

import com.peto.ramap.domain.model.notification.EventNotificationOverride
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventNotificationOverrideResponse(
    @SerialName("event_id") val eventId: String,
    val enabled: Boolean,
) {
    fun toDomain() = EventNotificationOverride(eventId, enabled)
}
