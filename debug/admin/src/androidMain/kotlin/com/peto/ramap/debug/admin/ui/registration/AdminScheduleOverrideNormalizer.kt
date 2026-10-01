package com.peto.ramap.debug.admin.ui.registration

import com.peto.ramap.debug.admin.data.model.AdminScheduleOverride

internal fun AdminScheduleOverride.segment(
    start: String,
    end: String,
) = copy(closed = false, open = start, close = end, closeNextDay = end < start, breakTimes = emptyList())

internal fun AdminScheduleOverride.continuousSegments(): List<AdminScheduleOverride> {
    val opening = open?.toMinute() ?: return emptyList()
    val closing = close?.toMinute() ?: return emptyList()
    val closingAt = closing + if (closeNextDay) MINUTES_PER_DAY else 0
    if (opening >= closingAt) return emptyList()
    var cursor = opening
    val segments =
        buildList {
            breakTimes
                .mapNotNull { breakTime ->
                    val start = breakTime.start.toMinute() ?: return@mapNotNull null
                    val end = breakTime.end.toMinute() ?: return@mapNotNull null
                    val startAt = start + if (start < opening) MINUTES_PER_DAY else 0
                    val endAt = end + if (end < opening || end <= start) MINUTES_PER_DAY else 0
                    startAt to endAt
                }.sortedBy { it.first }
                .forEach { breakTime ->
                    val breakStart = maxOf(opening, breakTime.first)
                    val breakEnd = minOf(closingAt, breakTime.second)
                    if (breakStart >= breakEnd) return@forEach
                    if (cursor < breakStart) add(segment(cursor, breakStart))
                    cursor = maxOf(cursor, breakEnd)
                }
            if (cursor < closingAt) add(segment(cursor, closingAt))
        }
    return segments
}

private fun AdminScheduleOverride.segment(
    start: Int,
    end: Int,
): AdminScheduleOverride =
    copy(
        closed = false,
        open = start.toTime(),
        close = end.toTime(),
        closeNextDay = end >= MINUTES_PER_DAY,
        breakTimes = emptyList(),
    )

private fun String.toMinute(): Int? =
    runCatching {
        val time = java.time.LocalTime.parse(this)
        time.hour * 60 + time.minute
    }.getOrNull()

private fun Int.toTime(): String = "%02d:%02d".format((this % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY / 60, this % 60)

private const val MINUTES_PER_DAY = 24 * 60
