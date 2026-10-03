package com.peto.ramap.domain.model.notice

import com.peto.ramap.domain.model.businesshour.BusinessDay
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import com.peto.ramap.domain.model.businesshour.BusinessHoursScheduleOverride
import kotlinx.datetime.LocalDate

data class OperatingNoticeDay(
    val date: LocalDate,
    val closed: Boolean,
    val scheduleOverride: BusinessHoursScheduleOverride? = null,
) {
    fun resolve(hours: BusinessHours?): BusinessHoursScheduleOverride {
        if (closed) return BusinessHoursScheduleOverride(BusinessHoursDay(true, null, null, false, null))
        scheduleOverride?.let { return it }
        val key = BusinessDay.from(date.dayOfWeek).key
        val regularDay = hours?.weekly?.get(key)?.takeUnless { it.closed }
        return BusinessHoursScheduleOverride(
            day = regularDay ?: BusinessHoursDay(false, null, null, false, null),
            breakTimes = if (regularDay != null) hours.breakTimes[key].orEmpty() else emptyList(),
            useRegularLastOrders = regularDay != null,
        )
    }
}
