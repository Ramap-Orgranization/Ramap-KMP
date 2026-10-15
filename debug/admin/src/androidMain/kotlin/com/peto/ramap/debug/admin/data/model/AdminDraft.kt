package com.peto.ramap.debug.admin.data.model

import com.peto.ramap.domain.model.notice.OperatingNoticeType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
internal data class AdminDraft(
    @SerialName("shop_name") val shopName: String? = null,
    val title: String? = null,
    @SerialName("event_type") val eventType: String? = null,
    @SerialName("venue_name") val venueName: String? = null,
    @SerialName("venue_address") val venueAddress: String? = null,
    @SerialName("external_venue_id") val externalVenueId: String? = null,
    @SerialName("venue_instagram_url") val venueInstagramUrl: String? = null,
    @SerialName("venue_naver_map_url") val venueNaverMapUrl: String? = null,
    @SerialName("venue_kakao_map_url") val venueKakaoMapUrl: String? = null,
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
    @SerialName("daily_schedules") val dailySchedules: List<AdminNoticeDay> = emptyList(),
    @SerialName("schedule_override") val scheduleOverride: AdminScheduleOverride? = null,
) {
    fun canRegisterOperatingNotice(): Boolean {
        val start = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
        val end = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
        if (end < start) return false
        if (startTime != null || scheduleOverride != null) return false
        val type = noticeType?.let(OperatingNoticeType::fromOrNull) ?: return false
        if (type == OperatingNoticeType.EARLY_CLOSING) {
            return endTime?.matches(Regex("([01][0-9]|2[0-3]):[0-5][0-9]")) == true && dailySchedules.isEmpty()
        }
        return endTime == null && dailySchedules.all { it.scheduleOverride == null } && (dailySchedules.isEmpty() || dailySchedules.any { it.closed })
    }
}
