package com.peto.ramap.data.model

import com.peto.ramap.data.extension.toLocalDate
import com.peto.ramap.data.extension.toLocalTime
import com.peto.ramap.domain.model.businesshour.BusinessHoursScheduleOverride
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.model.shop.RamenShop
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
internal data class OperatingNoticeResponse(
    val id: String,
    @SerialName("shop_id")
    val shopId: String,
    @SerialName("notice_type")
    val noticeType: String,
    val description: String,
    @SerialName("notice_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String?,
    @SerialName("start_time")
    val startTime: String?,
    @SerialName("end_time")
    val endTime: String?,
    @SerialName("schedule_override")
    val scheduleOverride: ScheduleOverrideResponse? = null,
    @SerialName("manually_released_at")
    val manuallyReleasedAt: Instant? = null,
    @SerialName("source_url")
    val sourceUrl: String?,
    @SerialName("updated_at")
    val updatedAt: String? = null,
) {
    fun toDomain(shop: RamenShop): OperatingNotice =
        OperatingNotice(
            id = id,
            shop = shop,
            type = OperatingNoticeType.from(noticeType),
            description = description,
            startDate = startDate.toLocalDate(),
            endDate = endDate?.toLocalDate(),
            startTime = startTime?.toLocalTime(),
            endTime = endTime?.toLocalTime(),
            scheduleOverride = scheduleOverride?.toDomain(),
            manuallyReleasedAt = manuallyReleasedAt,
            sourceUrl = sourceUrl,
            updatedAt = updatedAt,
        )
}

@Serializable
internal data class ScheduleOverrideResponse(
    val closed: Boolean = false,
    val open: String? = null,
    val close: String? = null,
    @SerialName("close_next_day") val closeNextDay: Boolean = false,
    val label: String? = null,
    @SerialName("break_times") val breakTimes: List<BreakTimeResponse> = emptyList(),
) {
    fun toDomain(): BusinessHoursScheduleOverride {
        require(closed || (open.isValidTime() && close.isValidTime()))
        require(breakTimes.all { it.start.isValidTime() && it.end.isValidTime() })
        return BusinessHoursScheduleOverride(
            day = BusinessHoursDayResponse(closed, open, close, closeNextDay, label).toDomain(),
            breakTimes = breakTimes.map(BreakTimeResponse::toDomain),
        )
    }
}

private fun String?.isValidTime(): Boolean = this != null && runCatching { LocalTime.parse(this) }.isSuccess
