package com.peto.ramap.domain.model.shop

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class RamenShop(
    val id: String,
    val kakaoPlaceId: String?,
    val name: String,
    val address: String,
    val location: Location,
    val kakaoPlaceUrl: String?,
    val naverPlaceUrl: String? = null,
    val phone: String?,
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
        currentDateTime: LocalDateTime =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    ): Boolean =
        (!filter.hasCategoryFilter || menuCategories.any { it in filter }) &&
            (!filter.isOpenSelected || isOpenAt(currentDateTime))

    fun isOpenAt(currentDateTime: LocalDateTime): Boolean {
        val businessHours = businessHoursDetails ?: return false
        val todayKey = dayKey(currentDateTime.date.dayOfWeek)
        val today = businessHours.weekly[todayKey]
        if (isOpenDuring(
                today,
                currentDateTime.time,
                businessHours.breakTimes[todayKey],
                false,
            )
        ) {
            return true
        }

        val previousKey = previousDayKey(currentDateTime.date.dayOfWeek)
        val previous = businessHours.weekly[previousKey]
        return isOpenDuring(
            previous,
            currentDateTime.time,
            businessHours.breakTimes[previousKey],
            true,
        )
    }

    private fun isOpenDuring(
        businessHoursDay: BusinessHoursDay?,
        currentTime: LocalTime,
        breakTimes: List<BusinessHoursBreakTime>?,
        isAfterMidnight: Boolean,
    ): Boolean {
        if (businessHoursDay == null || businessHoursDay.closed) return false
        val open = businessHoursDay.open?.let(::parseTime) ?: return false
        val close = businessHoursDay.close?.let(::parseTime) ?: return false
        val isWithinHours =
            if (businessHoursDay.closeNextDay) {
                if (isAfterMidnight) currentTime < close else currentTime >= open
            } else {
                currentTime >= open && currentTime < close
            }
        if (!isWithinHours) return false
        val parsedBreakTimes =
            breakTimes.orEmpty().map { breakTime ->
                val start = parseTime(breakTime.start) ?: return false
                val end = parseTime(breakTime.end) ?: return false
                start to end
            }
        return parsedBreakTimes.none { (start, end) -> isWithinBreakTime(currentTime, start, end) }
    }

    private fun isWithinBreakTime(
        currentTime: LocalTime,
        start: LocalTime,
        end: LocalTime,
    ): Boolean {
        if (start == end) return false
        return if (start < end) {
            currentTime >= start && currentTime < end
        } else {
            currentTime >= start || currentTime < end
        }
    }

    private fun parseTime(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()

    private fun dayKey(dayOfWeek: DayOfWeek): String =
        when (dayOfWeek) {
            DayOfWeek.MONDAY -> "mon"
            DayOfWeek.TUESDAY -> "tue"
            DayOfWeek.WEDNESDAY -> "wed"
            DayOfWeek.THURSDAY -> "thu"
            DayOfWeek.FRIDAY -> "fri"
            DayOfWeek.SATURDAY -> "sat"
            DayOfWeek.SUNDAY -> "sun"
        }

    private fun previousDayKey(dayOfWeek: DayOfWeek): String =
        when (dayOfWeek) {
            DayOfWeek.MONDAY -> "sun"
            DayOfWeek.TUESDAY -> "mon"
            DayOfWeek.WEDNESDAY -> "tue"
            DayOfWeek.THURSDAY -> "wed"
            DayOfWeek.FRIDAY -> "thu"
            DayOfWeek.SATURDAY -> "fri"
            DayOfWeek.SUNDAY -> "sat"
        }
}
