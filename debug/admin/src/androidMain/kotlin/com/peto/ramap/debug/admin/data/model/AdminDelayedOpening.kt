package com.peto.ramap.debug.admin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdminDelayedOpening(
    val id: String,
    @SerialName("notice_date") val noticeDate: String,
    @SerialName("start_time") val startTime: String? = null,
    val description: String,
    @SerialName("manually_released_at") val manuallyReleasedAt: String? = null,
)
