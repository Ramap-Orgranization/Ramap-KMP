package com.peto.ramap.domain.model.notification

enum class EventNotificationWindow {
    DAY_BEFORE_AND_EVENT_DAY,
    EVENT_DAY_ONLY,
    CLOSED,
}

fun eventNotificationWindow(
    startDate: String,
    nowEpochMillis: Long,
): EventNotificationWindow {
    val parts = startDate.split('-').mapNotNull(String::toIntOrNull)
    if (parts.size != 3) return EventNotificationWindow.CLOSED
    val eventDayNineAmEpochMillis = daysFromCivil(parts[0], parts[1], parts[2]) * MILLIS_PER_DAY
    val previousDayNinePmEpochMillis = eventDayNineAmEpochMillis - TWELVE_HOURS_MILLIS
    return when {
        nowEpochMillis >= eventDayNineAmEpochMillis -> EventNotificationWindow.CLOSED
        nowEpochMillis >= previousDayNinePmEpochMillis -> EventNotificationWindow.EVENT_DAY_ONLY
        else -> EventNotificationWindow.DAY_BEFORE_AND_EVENT_DAY
    }
}

private fun daysFromCivil(
    year: Int,
    month: Int,
    day: Int,
): Long {
    val adjustedYear = year - if (month <= 2) 1 else 0
    val era = adjustedYear / 400
    val yearOfEra = adjustedYear - era * 400
    val adjustedMonth = month + if (month > 2) -3 else 9
    val dayOfYear = (153 * adjustedMonth + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return (era * 146097L + dayOfEra - 719468L)
}

private const val MILLIS_PER_DAY = 86_400_000L
private const val TWELVE_HOURS_MILLIS = 43_200_000L
