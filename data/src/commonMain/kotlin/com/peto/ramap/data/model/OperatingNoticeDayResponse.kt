package com.peto.ramap.data.model

import com.peto.ramap.domain.model.notice.OperatingNoticeDay
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OperatingNoticeDayResponse(
    val date: String,
    val closed: Boolean,
    @SerialName("schedule_override") val scheduleOverride: ScheduleOverrideResponse? = null,
) {
    fun toDomain(): OperatingNoticeDay {
        require(scheduleOverride == null || (!closed && !scheduleOverride.closed))
        return OperatingNoticeDay(LocalDate.parse(date), closed, scheduleOverride?.toDomain())
    }
}
