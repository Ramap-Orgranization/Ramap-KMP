package com.peto.ramap.domain.model.businesshour

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

internal object BusinessHoursStatusCalculator {
    /**
     * 주어진 시각의 영업 상태를 계산한다.
     *
     * 영업 중이면 마지막 주문 시간 또는 마감 시간을 포함한 상태를 반환하고,
     * 휴게시간이면 휴게 종료 시간을, 영업 전이면 다음 영업시간을 반환한다.
     */
    fun statusAt(
        businessHours: BusinessHours,
        currentDateTime: LocalDateTime,
    ): BusinessHoursStatus? {
        val activeDate = findActiveBusinessDate(businessHours, currentDateTime)
        if (activeDate != null) {
            return openStatus(businessHours, activeDate, currentDateTime.time)
        }

        val breakEndTime = findBreakEndDuringCurrentHours(businessHours, currentDateTime)
        if (breakEndTime != null) return BusinessHoursStatus.BreakTime(breakEndTime)

        val today = businessHours.dayAt(currentDateTime.date)
        if (currentDateTime.date in businessHours.dateOverrides && today != null && !today.closed && (today.open == null || today.close == null)) {
            return BusinessHoursStatus.Unknown
        }
        val nextOpenTime = findNextOpeningSchedule(businessHours, currentDateTime)
        return nextOpenTime?.let(BusinessHoursStatus::Closed)
    }

    /**
     * 영업 중인 매장의 상태를 마지막 주문 시간과 마감 시간 기준으로 계산한다.
     */
    private fun openStatus(
        businessHours: BusinessHours,
        date: LocalDate,
        currentTime: LocalTime,
    ): BusinessHoursStatus {
        val lastOrder = findActiveLastOrder(businessHours, date, currentTime)
        if (lastOrder != null) return BusinessHoursStatus.OpenWithLastOrder(lastOrder)

        val closeTime = businessHours.dayAt(date)?.close ?: return BusinessHoursStatus.Open

        return BusinessHoursStatus.OpenUntil(closeTime)
    }

    private fun findActiveLastOrder(
        businessHours: BusinessHours,
        date: LocalDate,
        currentTime: LocalTime,
    ): String? {
        val lastOrders = businessHours.lastOrdersAt(date)
        val nextBreakIndex =
            businessHours.breaksAt(date).indexOfFirst { breakTime ->
                parseTime(breakTime.start)?.let { currentTime < it } == true
            }
        val activeLastOrderIndex = nextBreakIndex.takeIf { it >= 0 } ?: lastOrders.lastIndex
        return lastOrders.getOrNull(activeLastOrderIndex) ?: lastOrders.lastOrNull()
    }

    /**
     * 현재 시각에 적용되는 영업일의 요일 키를 찾는다.
     *
     * 당일 일정과 전날의 익일 마감 일정을 순서대로 확인한다.
     */
    private fun findActiveBusinessDate(
        businessHours: BusinessHours,
        currentDateTime: LocalDateTime,
    ): LocalDate? {
        val today = currentDateTime.date
        if (isOpenDuring(
                businessHours = businessHours,
                date = today,
                currentTime = currentDateTime.time,
                isAfterMidnight = false,
            )
        ) {
            return today
        }

        val previous = today.minus(1, DateTimeUnit.DAY)
        if (isOpenDuring(
                businessHours = businessHours,
                date = previous,
                currentTime = currentDateTime.time,
                isAfterMidnight = true,
            )
        ) {
            return previous
        }

        return null
    }

    /**
     * 현재 시각이 휴게시간이면 해당 휴게시간의 종료 시각을 찾는다.
     */
    private fun findBreakEndDuringCurrentHours(
        businessHours: BusinessHours,
        currentDateTime: LocalDateTime,
    ): String? {
        val today = currentDateTime.date
        val todayBreakEnd =
            findBreakEndIfWithinHours(
                businessHours = businessHours,
                date = today,
                currentTime = currentDateTime.time,
                isAfterMidnight = false,
            )
        if (todayBreakEnd != null) return todayBreakEnd

        val previous = today.minus(1, DateTimeUnit.DAY)
        return findBreakEndIfWithinHours(
            businessHours = businessHours,
            date = previous,
            currentTime = currentDateTime.time,
            isAfterMidnight = true,
        )
    }

    /**
     * 특정 영업일의 영업시간 안에 현재 시각이 있는 경우 휴게 종료 시각을 찾는다.
     */
    private fun findBreakEndIfWithinHours(
        businessHours: BusinessHours,
        date: LocalDate,
        currentTime: LocalTime,
        isAfterMidnight: Boolean,
    ): String? {
        val businessHoursDay = businessHours.dayAt(date)
        if (!isWithinBusinessHours(businessHoursDay, currentTime, isAfterMidnight)) return null

        return findBreakEnd(businessHours.breaksAt(date), currentTime)
    }

    /**
     * 오늘부터 일주일 안에서 현재 시각 이후의 다음 영업 시작 시각을 찾는다.
     */
    private fun findNextOpeningSchedule(
        businessHours: BusinessHours,
        currentDateTime: LocalDateTime,
    ): String? {
        for (dayOffset in 0..7) {
            val date = currentDateTime.date.plus(dayOffset, DateTimeUnit.DAY)
            val openingTime =
                findOpeningTime(
                    businessHours = businessHours,
                    date = date,
                    dayOffset = dayOffset,
                    currentTime = currentDateTime.time,
                )
            if (openingTime != null) return openingTime
        }
        return null
    }

    /**
     * 주어진 날짜의 영업 시작 시각이 현재 시각 이후인지 확인한다.
     */
    private fun findOpeningTime(
        businessHours: BusinessHours,
        date: LocalDate,
        dayOffset: Int,
        currentTime: LocalTime,
    ): String? {
        val day = businessHours.dayAt(date) ?: return null
        if (day.closed) return null

        val openTime = day.open ?: return null
        val parsedOpenTime = parseTime(openTime) ?: return null
        if (dayOffset == 0 && parsedOpenTime <= currentTime) return null

        return openTime
    }

    /**
     * 현재 시각이 영업시간 범위 안에 있는지 확인한다.
     *
     * [isAfterMidnight]가 true이면 전날 영업이 자정을 넘어 이어지는 상황으로 처리한다.
     */
    private fun isWithinBusinessHours(
        businessHoursDay: BusinessHoursDay?,
        currentTime: LocalTime,
        isAfterMidnight: Boolean,
    ): Boolean {
        if (businessHoursDay == null || businessHoursDay.closed) return false
        if (isAfterMidnight && !businessHoursDay.closeNextDay) return false

        val openValue = businessHoursDay.open ?: return false
        val closeValue = businessHoursDay.close ?: return false
        val openTime = parseTime(openValue) ?: return false
        val closeTime = parseTime(closeValue) ?: return false
        return if (businessHoursDay.closeNextDay) {
            if (isAfterMidnight) currentTime < closeTime else currentTime >= openTime
        } else {
            currentTime in openTime..<closeTime
        }
    }

    /**
     * 현재 시각이 포함된 휴게시간의 종료 시각을 찾는다.
     */
    private fun findBreakEnd(
        breakTimes: List<BreakTime>?,
        currentTime: LocalTime,
    ): String? {
        for (breakTime in breakTimes.orEmpty()) {
            if (isWithinBreakTime(currentTime, breakTime)) return breakTime.end
        }
        return null
    }

    /**
     * 현재 시각이 특정 휴게시간 안에 있는지 확인한다.
     *
     * 시작 시각이 종료 시각보다 늦으면 자정을 넘기는 휴게시간으로 처리한다.
     */
    private fun isWithinBreakTime(
        currentTime: LocalTime,
        breakTime: BreakTime,
    ): Boolean {
        val startTime = parseTime(breakTime.start) ?: return false
        val endTime = parseTime(breakTime.end) ?: return false
        if (startTime == endTime) return false

        return if (startTime < endTime) {
            currentTime in startTime..<endTime
        } else {
            currentTime !in endTime..<startTime
        }
    }

    /**
     * 특정 영업일의 영업시간과 휴게시간을 모두 고려해 영업 여부를 확인한다.
     */
    private fun isOpenDuring(
        businessHours: BusinessHours,
        date: LocalDate,
        currentTime: LocalTime,
        isAfterMidnight: Boolean,
    ): Boolean {
        val businessHoursDay = businessHours.dayAt(date)
        if (!isWithinBusinessHours(businessHoursDay, currentTime, isAfterMidnight)) return false

        return !isDuringBreak(businessHours.breaksAt(date), currentTime)
    }

    /**
     * 현재 시각이 등록된 휴게시간 중 하나에 포함되는지 확인한다.
     */
    private fun isDuringBreak(
        breakTimes: List<BreakTime>?,
        currentTime: LocalTime,
    ): Boolean = breakTimes.orEmpty().any { breakTime -> isWithinBreakTime(currentTime, breakTime) }

    /**
     * 문자열 형식의 시각을 [LocalTime]으로 변환한다.
     *
     * 형식이 올바르지 않으면 null을 반환한다.
     */
    private fun parseTime(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()
}
