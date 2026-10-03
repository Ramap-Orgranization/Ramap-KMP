package com.peto.ramap.debug.admin.data.model.request

import com.peto.ramap.debug.admin.data.model.AdminNoticeDay
import com.peto.ramap.debug.admin.data.model.AdminParticipant
import com.peto.ramap.debug.admin.data.model.AdminScheduleOverride
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RegisterRequest(
    @SerialName("registration_type") val registrationType: String,
    @SerialName("shop_name") val shopName: String,
    val title: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("venue_name") val venueName: String? = null,
    @SerialName("venue_address") val venueAddress: String? = null,
    @SerialName("external_venue_id") val externalVenueId: String? = null,
    @SerialName("venue_instagram_url") val venueInstagramUrl: String? = null,
    @SerialName("venue_naver_map_url") val venueNaverMapUrl: String? = null,
    @SerialName("venue_kakao_map_url") val venueKakaoMapUrl: String? = null,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String?,
    val description: String,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("evidence_path") val evidencePath: String?,
    @SerialName("notice_type") val noticeType: String?,
    @SerialName("start_time") val startTime: String?,
    @SerialName("end_time") val endTime: String?,
    @SerialName("daily_schedules") val dailySchedules: List<AdminNoticeDay> = emptyList(),
    @SerialName("schedule_override") val scheduleOverride: AdminScheduleOverride? = null,
    @SerialName("image_only") val imageOnly: Boolean = false,
    val participants: List<AdminParticipant> = emptyList(),
)
