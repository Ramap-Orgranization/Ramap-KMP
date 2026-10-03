package com.peto.ramap.debug.admin.data.model

import com.peto.ramap.domain.model.businesshour.BreakTime
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import com.peto.ramap.domain.model.businesshour.BusinessHoursScheduleOverride
import com.peto.ramap.domain.model.notice.OperatingNoticeDay
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdminNoticeDay(
    val date: String,
    val closed: Boolean,
    @SerialName("schedule_override") val scheduleOverride: AdminScheduleOverride? = null,
) {
    fun toDomain(): OperatingNoticeDay =
        OperatingNoticeDay(
            date = LocalDate.parse(date),
            closed = closed,
            scheduleOverride =
                scheduleOverride?.let {
                    BusinessHoursScheduleOverride(
                        day = BusinessHoursDay(it.closed, it.open, it.close, it.closeNextDay, it.label),
                        breakTimes = it.breakTimes.map { period -> BreakTime(period.start, period.end) },
                    )
                },
        )
}
