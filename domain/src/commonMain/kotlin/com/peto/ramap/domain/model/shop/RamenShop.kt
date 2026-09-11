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
            status !is BusinessHoursStatus.Closed && status !is BusinessHoursStatus.BreakTime
        } == true

    fun businessHoursStatus(
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice> = emptyList(),
    ): BusinessHoursStatus? {
        val hours = businessHoursDetails ?: return null
        val applicableNotices = operatingNotices.filter { it.shop.id == id }
        val status = effectiveBusinessHours(hours, currentDateTime, applicableNotices).statusAt(currentDateTime) ?: return null
        return if (hasOperatingNoticeBlockingOpening(currentDateTime, applicableNotices)) {
            BusinessHoursStatus.Closed()
        } else {
            status
        }
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
                    OperatingNoticeType.EARLY_CLOSING ->
                        notice.endTime?.let { currentDateTime >= LocalDateTime(businessDate, it) } == true
                    OperatingNoticeType.LATE_OPENING ->
                        notice.manuallyReleasedAt == null &&
                            (notice.startTime == null || currentDateTime < LocalDateTime(businessDate, notice.startTime))
                    OperatingNoticeType.OPERATING_NOTICE -> false
                }
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
        val weekly = businessHours.weekly.toMutableMap()
        val dates = listOf(currentDateTime.date, currentDateTime.date.minus(1, DateTimeUnit.DAY))
        for (date in dates) {
            val override = notices.latestScheduleOverrideFor(date, currentDateTime.time) ?: continue
            weekly[BusinessDay.from(date.dayOfWeek).key] = override.scheduleOverride!!.day
        }
        val breakTimes = businessHours.breakTimes.toMutableMap()
        for (date in dates) {
            val override = notices.latestScheduleOverrideFor(date, currentDateTime.time) ?: continue
            breakTimes[BusinessDay.from(date.dayOfWeek).key] = override.scheduleOverride!!.breakTimes
        }
        return businessHours.copy(weekly = weekly, breakTimes = breakTimes)
    }

    fun latestScheduleOverride(
        currentDateTime: LocalDateTime,
        operatingNotices: List<OperatingNotice>,
    ): OperatingNotice? =
        operatingNotices
            .filter { it.shop.id == id }
            .latestScheduleOverrideFor(currentDateTime.date, currentDateTime.time)

    private fun List<OperatingNotice>.latestScheduleOverrideFor(
        date: LocalDate,
        time: LocalTime,
    ): OperatingNotice? =
        asSequence()
            .filter {
                it.type == OperatingNoticeType.OPERATING_NOTICE &&
                    it.scheduleOverride != null &&
                    it.isActiveAt(LocalDateTime(date, time))
            }.maxByOrNull { it.updatedAt.orEmpty() }
}
