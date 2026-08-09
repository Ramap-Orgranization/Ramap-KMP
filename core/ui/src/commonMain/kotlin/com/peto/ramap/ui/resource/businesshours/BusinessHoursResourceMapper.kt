package com.peto.ramap.ui.resource.businesshours

import com.peto.ramap.domain.model.shop.BusinessHours
import com.peto.ramap.domain.model.shop.BusinessHoursDay
import com.peto.ramap.ui.resource.UiText
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_detail_business_hours_break_time_format
import ramap.shared.generated.resources.shop_detail_business_hours_closed
import ramap.shared.generated.resources.shop_detail_business_hours_closed_label_format
import ramap.shared.generated.resources.shop_detail_business_hours_last_order_format
import ramap.shared.generated.resources.shop_detail_business_hours_next_day_time_format
import ramap.shared.generated.resources.shop_detail_business_hours_time_format

object BusinessHoursResourceMapper {
    fun today(
        businessHours: BusinessHours,
        dayKey: String,
    ): List<BusinessHoursResourceLine> = lines(businessHours, listOf(dayKey))

    fun all(businessHours: BusinessHours): List<BusinessHoursResourceLine> {
        val lines = mutableListOf<BusinessHoursResourceLine>()
        var startDayKey: String? = null
        var endDayKey: String? = null
        var previousDay: BusinessHoursDay? = null
        var previousValues: List<UiText>? = null

        fun addCurrentLine() {
            val startKey = startDayKey ?: return
            val values = previousValues ?: return
            val startDayResource = DAY_RESOURCES.getValue(startKey).resource
            lines +=
                BusinessHoursResourceLine(
                    dayLabel = startDayResource,
                    endDayLabel = endDayKey?.let(DAY_RESOURCES::getValue)?.resource,
                    values = values,
                )
        }

        for (weekday in BusinessHoursWeekday.entries) {
            val dayKey = weekday.key
            val day = businessHours.weekly[dayKey]
            if (day == null) {
                addCurrentLine()
                startDayKey = null
                endDayKey = null
                previousDay = null
                previousValues = null
                continue
            }

            val dayValues = values(businessHours, dayKey, day)
            if (dayValues.isEmpty()) {
                addCurrentLine()
                startDayKey = null
                endDayKey = null
                previousDay = null
                previousValues = null
                continue
            }

            val canExtend =
                previousDay != null &&
                    previousValues == dayValues &&
                    previousDay == day
            if (!canExtend) {
                addCurrentLine()
                startDayKey = dayKey
            }
            endDayKey = dayKey.takeIf { canExtend }
            previousDay = day
            previousValues = dayValues
        }
        addCurrentLine()
        return lines
    }

    private fun lines(
        businessHours: BusinessHours,
        dayKeys: Collection<String>,
    ): List<BusinessHoursResourceLine> =
        dayKeys.mapNotNull { dayKey ->
            val dayResource = DAY_RESOURCES[dayKey]?.resource ?: return@mapNotNull null
            val day = businessHours.weekly[dayKey] ?: return@mapNotNull null
            val values = values(businessHours, dayKey, day)
            values.takeIf(List<UiText>::isNotEmpty)?.let {
                BusinessHoursResourceLine(dayLabel = dayResource, values = it)
            }
        }

    private fun values(
        businessHours: BusinessHours,
        dayKey: String,
        day: BusinessHoursDay,
    ): List<UiText> =
        buildList {
            if (day.closed) {
                day.label
                    ?.takeIf(String::isNotBlank)
                    ?.let { add(UiText(Res.string.shop_detail_business_hours_closed_label_format, listOf(it))) }
                    ?: add(UiText(Res.string.shop_detail_business_hours_closed))
            } else {
                val open = day.open
                val close = day.close
                if (open == null || close == null) {
                    return@buildList
                }
                add(
                    UiText(
                        resource =
                            if (day.closeNextDay) {
                                Res.string.shop_detail_business_hours_next_day_time_format
                            } else {
                                Res.string.shop_detail_business_hours_time_format
                            },
                        arguments = listOf(open, close),
                    ),
                )
            }
            businessHours.breakTimes[dayKey].orEmpty().forEach { breakTime ->
                add(
                    UiText(
                        Res.string.shop_detail_business_hours_break_time_format,
                        listOf(breakTime.start, breakTime.end),
                    ),
                )
            }
            businessHours.lastOrders[dayKey].orEmpty().forEach { lastOrder ->
                add(UiText(Res.string.shop_detail_business_hours_last_order_format, listOf(lastOrder)))
            }
        }

    private val DAY_RESOURCES = BusinessHoursWeekday.entries.associateBy(BusinessHoursWeekday::key)
}
