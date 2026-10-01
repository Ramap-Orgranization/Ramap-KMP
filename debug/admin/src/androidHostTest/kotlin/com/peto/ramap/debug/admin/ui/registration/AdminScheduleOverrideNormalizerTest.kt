package com.peto.ramap.debug.admin.ui.registration

import com.peto.ramap.debug.admin.data.model.AdminBreakTime
import com.peto.ramap.debug.admin.data.model.AdminScheduleOverride
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminScheduleOverrideNormalizerTest {
    @Test
    fun `break 전후 구간 선택은 중단 없는 변경 영업시간으로 정규화된다`() {
        val regular =
            AdminScheduleOverride(
                open = "11:00",
                close = "21:00",
                breakTimes = listOf(AdminBreakTime(start = "15:00", end = "17:00")),
            )

        assertEquals(
            AdminScheduleOverride(open = "11:00", close = "15:00"),
            regular.segment(start = "11:00", end = "15:00"),
        )
        assertEquals(
            AdminScheduleOverride(open = "17:00", close = "21:00"),
            regular.segment(start = "17:00", end = "21:00"),
        )
    }

    @Test
    fun `여러 휴게시간은 실제 연속 영업 구간만 만든다`() {
        val regular =
            AdminScheduleOverride(
                open = "11:00",
                close = "22:00",
                breakTimes =
                    listOf(
                        AdminBreakTime(start = "17:00", end = "18:00"),
                        AdminBreakTime(start = "14:00", end = "15:00"),
                    ),
            )

        assertEquals(
            listOf(
                AdminScheduleOverride(open = "11:00", close = "14:00"),
                AdminScheduleOverride(open = "15:00", close = "17:00"),
                AdminScheduleOverride(open = "18:00", close = "22:00"),
            ),
            regular.continuousSegments(),
        )
    }

    @Test
    fun `익일 마감 일정의 휴게시간은 각 구간의 익일 마감을 정규화한다`() {
        val regular =
            AdminScheduleOverride(
                open = "18:00",
                close = "02:00",
                closeNextDay = true,
                breakTimes = listOf(AdminBreakTime(start = "22:00", end = "23:00")),
            )

        assertEquals(
            listOf(
                AdminScheduleOverride(open = "18:00", close = "22:00"),
                AdminScheduleOverride(open = "23:00", close = "02:00", closeNextDay = true),
            ),
            regular.continuousSegments(),
        )
    }
}
