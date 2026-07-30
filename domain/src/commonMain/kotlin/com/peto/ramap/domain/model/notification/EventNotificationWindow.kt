package com.peto.ramap.domain.model.notification

/**
 * 이벤트 알림 설정이 가능한 시간 창을 나타낸다.
 *
 * 모든 시간 계산은 KST(UTC+9)를 기준으로 한다.
 * UTC 자정(00:00 UTC)이 KST 오전 9시이므로, UTC epoch 기반 계산에
 * 별도 오프셋을 더하지 않아도 KST 기준 시간이 된다.
 */
enum class EventNotificationWindow {
    DAY_BEFORE_AND_EVENT_DAY,
    EVENT_DAY_ONLY,
    CLOSED,
    ;

    companion object {
        /** KST(UTC+9) 기준: UTC 자정 = KST 09:00 */
        private const val MILLIS_PER_DAY = 86_400_000L

        /** KST(UTC+9) 기준: UTC 정오 = KST 21:00 (전날 알림 시작 시각) */
        private const val TWELVE_HOURS_MILLIS = 43_200_000L

        /**
         * [startDate]와 현재 시각 [nowEpochMillis]를 기준으로 알림 설정 가능 창을 계산한다.
         *
         * KST 기준 이벤트 당일 09:00(= UTC 자정)부터 알림 설정이 닫히고,
         * 전날 21:00(= UTC 전날 정오)부터는 당일 알림만 설정할 수 있다.
         *
         * @param startDate `yyyy-MM-dd` 형식의 이벤트 시작일
         * @param nowEpochMillis 현재 UTC epoch 밀리초
         */
        fun from(
            startDate: String,
            nowEpochMillis: Long,
        ): EventNotificationWindow {
            val parts = startDate.split('-').mapNotNull(String::toIntOrNull)
            if (parts.size != 3) return CLOSED

            // UTC 자정 = KST 09:00
            val eventDayNineAmEpochMillis = daysFromCivil(parts[0], parts[1], parts[2]) * MILLIS_PER_DAY
            // UTC 전날 정오 = KST 전날 21:00
            val previousDayNinePmEpochMillis = eventDayNineAmEpochMillis - TWELVE_HOURS_MILLIS

            return when {
                nowEpochMillis >= eventDayNineAmEpochMillis -> CLOSED
                nowEpochMillis >= previousDayNinePmEpochMillis -> EVENT_DAY_ONLY
                else -> DAY_BEFORE_AND_EVENT_DAY
            }
        }

        /**
         * civil date를 epoch 기준 일 수로 변환한다.
         *
         * Howard Hinnant의 `days_from_civil` 알고리즘을 사용하며
         * 1970-01-01을 0으로 하는 UTC 일 수를 반환한다.
         */
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
            return era * 146097L + dayOfEra - 719468L
        }
    }
}
