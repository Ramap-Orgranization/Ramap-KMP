package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.businesshour.BusinessDay
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
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
        if (hasOperatingNoticeBlockingOpening(currentDateTime, applicableNotices)) return BusinessHoursStatus.Closed()
        val hours = businessHoursDetails ?: BusinessHours(emptyMap(), emptyMap(), emptyMap(), null)
        val effectiveHours = effectiveBusinessHours(hours, currentDateTime, applicableNotices)
        if (effectiveHours.dateOverrides[currentDateTime.date]?.day?.closed == true) return BusinessHoursStatus.Closed()
        return effectiveHours.statusAt(currentDateTime)
    }

    private fun hasOperatingNoticeBlockingOpening(
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice>,
    ): Boolean =
        operatingNotices
            .asSequence()
            .mapNotNull { notice -> notice.businessDateAt(currentDateTime)?.let { businessDate -> notice to businessDate } }
            .any { (notice, businessDate) ->
                when (notice.type) {
                    OperatingNoticeType.TEMPORARY_CLOSURE -> true
                    OperatingNoticeType.EARLY_CLOSING -> hasEarlyClosingStarted(notice, businessDate, currentDateTime)
                    OperatingNoticeType.LATE_OPENING ->
                        notice.manuallyReleasedAt == null &&
                            (notice.startTime == null || currentDateTime < LocalDateTime(businessDate, notice.startTime))
                    OperatingNoticeType.OPERATING_NOTICE -> false
                }
            }

    private fun hasEarlyClosingStarted(
        notice: OperatingNotice,
        businessDate: LocalDate,
        currentDateTime: LocalDateTime,
    ): Boolean {
        val endTime = notice.endTime ?: return false
        val businessDay = businessHoursDetails?.weekly?.get(BusinessDay.from(businessDate.dayOfWeek).key)
        val openTime = businessDay?.open?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val closingDate =
            if (businessDay?.closeNextDay == true && openTime != null && endTime < openTime) {
                businessDate.plus(1, DateTimeUnit.DAY)
            } else {
                businessDate
            }
        return currentDateTime >= LocalDateTime(closingDate, endTime)
    }

    private fun OperatingNotice.businessDateAt(currentDateTime: LocalDateTime) =
        when {
            isActiveAt(currentDateTime) -> currentDateTime.date
            appliesToPreviousDaySession(this, currentDateTime) -> currentDateTime.date.minus(1, DateTimeUnit.DAY)
            else -> null
        }

    private fun appliesToPreviousDaySession(
        notice: OperatingNotice,
        currentDateTime: LocalDateTime,
    ): Boolean {
        val previousDate = currentDateTime.date.minus(1, DateTimeUnit.DAY)
        if (notice.endDate != previousDate) return false
        val previousHours = businessHoursDetails?.weekly?.get(BusinessDay.from(previousDate.dayOfWeek).key) ?: return false
        if (!previousHours.closeNextDay) return false
        val closeTime = previousHours.close?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return false
        return currentDateTime.time < closeTime
    }

    private fun effectiveBusinessHours(
        businessHours: BusinessHours,
        currentDateTime: LocalDateTime,
        notices: List<OperatingNotice>,
    ): BusinessHours {
        val overrides = businessHours.dateOverrides.toMutableMap()
        for (offset in -1..7) {
            val date = currentDateTime.date.plus(offset, DateTimeUnit.DAY)
            val notice = latestScheduleOverrideFor(notices, date) ?: continue
            overrides[date] = notice.scheduleOn(date, businessHours) ?: continue
        }
        return businessHours.copy(dateOverrides = overrides)
    }

    fun latestScheduleOverride(
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice>,
    ): OperatingNotice? {
        val notice = latestScheduleOverrideFor(operatingNotices, currentDateTime.date) ?: return null
        return notice.copy(scheduleOverride = notice.scheduleOn(currentDateTime.date, businessHoursDetails))
    }

    private fun latestScheduleOverrideFor(
        notices: List<OperatingNotice>,
        date: LocalDate,
    ): OperatingNotice? =
        notices
            .filter {
                it.shop.id == id &&
                    it.type == OperatingNoticeType.OPERATING_NOTICE &&
                    it.scheduleOn(date, businessHoursDetails) != null
            }.maxWithOrNull(compareBy<OperatingNotice> { it.updatedAt.orEmpty() }.thenBy { it.id })
}
