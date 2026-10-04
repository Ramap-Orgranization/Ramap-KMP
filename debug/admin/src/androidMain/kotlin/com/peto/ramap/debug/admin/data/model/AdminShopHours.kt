package com.peto.ramap.debug.admin.data.model

import com.peto.ramap.domain.model.businesshour.BreakTime
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdminShopHours(
    @SerialName("business_hours_weekly") val weekly: Map<String, AdminScheduleOverride> = emptyMap(),
    @SerialName("business_hours_break_times") val breakTimes: Map<String, List<AdminBreakTime>> = emptyMap(),
) {
    fun toDomain(): BusinessHours =
        BusinessHours(
            weekly = weekly.mapValues { (_, day) -> BusinessHoursDay(day.closed, day.open, day.close, day.closeNextDay, day.label) },
            breakTimes = breakTimes.mapValues { (_, periods) -> periods.map { BreakTime(it.start, it.end) } },
            lastOrders = emptyMap(),
            notice = null,
        )
}
