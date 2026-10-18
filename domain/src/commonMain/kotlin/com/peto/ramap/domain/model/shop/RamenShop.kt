package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import com.peto.ramap.domain.model.businesshour.BusinessHoursScheduleOverride
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

data class RamenShop(
    val id: String,
    val name: String,
    val address: String,
    val location: Location,
    val kakaoPlaceUrl: String?,
    val naverPlaceUrl: String? = null,
    val instagramUrl: String?,
    val menuCategories: MenuCategories,
    val isVisible: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val instagramProfileImageUrl: String? = null,
    val businessHoursDetails: BusinessHours? = null,
) {
    val hasCategory: Boolean
        get() = menuCategories.hasCategory

    fun isOpened(
        filter: RamenShopFilter,
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice> = emptyList(),
    ): Boolean =
        (!filter.hasCategoryFilter || menuCategories.any { it in filter }) &&
            (!filter.isOpenSelected || isOpenAt(currentDateTime, operatingNotices))

    fun isOpenAt(
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice> = emptyList(),
    ): Boolean =
        businessHoursStatus(currentDateTime, operatingNotices)?.let { status ->
            !status.isNotOpening
        } == true

    fun businessHoursStatus(
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice> = emptyList(),
    ): BusinessHoursStatus? {
        val applicableNotices = operatingNotices.filter { it.shop.id == id }
        val hours = businessHoursDetails ?: BusinessHours(emptyMap(), emptyMap(), emptyMap(), null)
        val effectiveHours = effectiveBusinessHours(hours, currentDateTime, applicableNotices)
        val hasTemporaryClosure = applicableNotices.any { it.type == OperatingNoticeType.TEMPORARY_CLOSURE && it.businessDateAt(currentDateTime) != null }
        if (hasTemporaryClosure || effectiveHours.dateOverrides[currentDateTime.date]?.day?.closed == true) {
            return BusinessHoursStatus.Closed(effectiveHours.nextOpeningTimeAt(currentDateTime))
        }
        if (hasEarlyClosingNoticeBlockingOpening(currentDateTime, applicableNotices)) return BusinessHoursStatus.Closed()
        return effectiveHours.statusAt(currentDateTime)
    }

    private fun hasEarlyClosingNoticeBlockingOpening(
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice>,
    ): Boolean =
        operatingNotices
            .asSequence()
            .filter { it.type.hasClosingTime }
            .mapNotNull { notice -> notice.businessDateAt(currentDateTime)?.let { businessDate -> notice to businessDate } }
            .any { (notice, businessDate) ->
                notice.closingAt(businessDate)?.let { currentDateTime >= it } == true
            }

    private fun effectiveBusinessHours(
        businessHours: BusinessHours,
        currentDateTime: LocalDateTime,
        notices: List<OperatingNotice>,
    ): BusinessHours {
        val overrides = businessHours.dateOverrides.toMutableMap()
        for (offset in -1..7) {
            val date = currentDateTime.date.plus(offset, DateTimeUnit.DAY)
            val closure = notices.firstOrNull { it.type == OperatingNoticeType.TEMPORARY_CLOSURE && it.appliesOn(date) }
            if (closure != null) {
                overrides[date] = BusinessHoursScheduleOverride(BusinessHoursDay(true, null, null, false, null))
                continue
            }
            val earlyClose = notices.filter { it.type.hasClosingTime && it.appliesOn(date) }.mapNotNull { it.closingAt(date) }.minOrNull() ?: continue
            val day = businessHours.dayAt(date) ?: continue
            val regularCloseTime = day.close?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: continue
            val regularCloseDate = if (day.closeNextDay) date.plus(1, DateTimeUnit.DAY) else date
            if (day.closed || earlyClose >= LocalDateTime(regularCloseDate, regularCloseTime)) continue
            overrides[date] =
                BusinessHoursScheduleOverride(
                    day = day.copy(close = earlyClose.time.toString(), closeNextDay = earlyClose.date > date),
                    breakTimes = businessHours.breaksAt(date),
                )
        }
        return businessHours.copy(dateOverrides = overrides)
    }
}
