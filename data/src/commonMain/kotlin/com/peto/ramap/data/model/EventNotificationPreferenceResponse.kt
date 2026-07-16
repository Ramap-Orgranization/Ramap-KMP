package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EventNotificationPreferenceResponse(
    val enabled: Boolean,
)
