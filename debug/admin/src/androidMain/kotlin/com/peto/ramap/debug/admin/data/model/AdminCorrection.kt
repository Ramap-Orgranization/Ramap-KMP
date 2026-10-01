package com.peto.ramap.debug.admin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdminCorrectionPreview(
    @SerialName("registration_type") val registrationType: String,
    @SerialName("target_id") val targetId: String,
    val summary: String,
    val changes: AdminCorrectionChanges,
)

@Serializable
internal data class AdminCorrectionChanges(
    val title: String? = null,
    val description: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("event_type") val eventType: String? = null,
    @SerialName("notice_type") val noticeType: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
)
