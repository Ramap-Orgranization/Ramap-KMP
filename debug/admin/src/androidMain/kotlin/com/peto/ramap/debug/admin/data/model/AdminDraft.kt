package com.peto.ramap.debug.admin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdminDraft(
    @SerialName("shop_name") val shopName: String? = null,
    val title: String? = null,
    @SerialName("event_type") val eventType: String? = null,
    val participants: List<AdminParticipant> = emptyList(),
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val description: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    val uncertainties: List<String> = emptyList(),
    @SerialName("evidence_path") val evidencePath: String? = null,
    @SerialName("notice_type") val noticeType: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
)
