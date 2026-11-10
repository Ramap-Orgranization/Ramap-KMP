package com.peto.ramap.datetime

import kotlinx.datetime.LocalDate

/**
 * [LocalDate]를 YYYY.MM.DD 형식의 문자열로 변환한다.
 */
fun LocalDate.toDotFormatted(): String {
    val yearStr = year.toString().padStart(4, '0')
    val monthStr = (month.ordinal + 1).toString().padStart(2, '0')
    val dayStr = dayOfMonth.toString().padStart(2, '0')
    return "$yearStr.$monthStr.$dayStr"
}

/**
 * 문자열을 [LocalDate]로 파싱한다. 실패 시 null을 반환한다.
 */
fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
