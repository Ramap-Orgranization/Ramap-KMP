package com.peto.ramap.debug.admin.data.model.request

import com.peto.ramap.debug.admin.data.model.AdminParticipant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RegisterRequest(
    @SerialName("registration_type") val registrationType: String,
    @SerialName("shop_name") val shopName: String,
    val title: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String?,
    val description: String,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("evidence_path") val evidencePath: String?,
    @SerialName("notice_type") val noticeType: String?,
    @SerialName("start_time") val startTime: String?,
    @SerialName("end_time") val endTime: String?,
    @SerialName("image_only") val imageOnly: Boolean = false,
    val participants: List<AdminParticipant> = emptyList(),
)
