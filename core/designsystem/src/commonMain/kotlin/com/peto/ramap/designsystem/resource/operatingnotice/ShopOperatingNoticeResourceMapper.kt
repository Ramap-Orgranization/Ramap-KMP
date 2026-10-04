package com.peto.ramap.designsystem.resource.operatingnotice

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeDay
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.operating_notice_day_closed
import ramap.shared.generated.resources.operating_notice_day_regular
import ramap.shared.generated.resources.operating_notice_hours_unknown
import ramap.shared.generated.resources.operating_notice_type_early_closing
import ramap.shared.generated.resources.operating_notice_type_late_opening
import ramap.shared.generated.resources.operating_notice_type_operating_notice
import ramap.shared.generated.resources.operating_notice_type_temporary_closure
import ramap.shared.generated.resources.shop_detail_business_hours_break_time_format
import ramap.shared.generated.resources.shop_detail_business_hours_next_day_time_format
import ramap.shared.generated.resources.shop_detail_business_hours_time_format

object ShopOperatingNoticeResourceMapper {
    fun daySchedule(
        day: OperatingNoticeDay,
        hours: BusinessHours?,
    ): List<UiText> {
        if (day.closed) return listOf(UiText(Res.string.operating_notice_day_closed))
        val schedule = day.resolve(hours)
        val open = schedule.day.open
        val close = schedule.day.close
        val regular = if (day.scheduleOverride == null) UiText(Res.string.operating_notice_day_regular) else null
        val time =
            if (open == null || close == null) {
                UiText(Res.string.operating_notice_hours_unknown)
            } else {
                UiText(
                    if (schedule.day.closeNextDay) Res.string.shop_detail_business_hours_next_day_time_format else Res.string.shop_detail_business_hours_time_format,
                    listOf(open, close),
                )
            }
        return listOfNotNull(regular, time) +
            schedule.breakTimes.map {
                UiText(Res.string.shop_detail_business_hours_break_time_format, listOf(it.start, it.end))
            }
    }

    fun typeLabel(type: OperatingNoticeType): StringResource =
        when (type) {
            OperatingNoticeType.OPERATING_NOTICE -> Res.string.operating_notice_type_operating_notice
            OperatingNoticeType.TEMPORARY_CLOSURE -> Res.string.operating_notice_type_temporary_closure
            OperatingNoticeType.EARLY_CLOSING -> Res.string.operating_notice_type_early_closing
            OperatingNoticeType.LATE_OPENING -> Res.string.operating_notice_type_late_opening
        }

    fun notice(notice: OperatingNotice): UiText = UiText(typeLabel(notice.type))
}
