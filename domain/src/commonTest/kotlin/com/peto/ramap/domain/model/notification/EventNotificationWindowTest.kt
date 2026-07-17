package com.peto.ramap.domain.model.notification

import kotlin.test.Test
import kotlin.test.assertEquals

class EventNotificationWindowTest {
    private val eventDayNineAm = 1783468800000L // 2026-07-08T00:00:00Z == 09:00 KST

    @Test
    fun `전날 오후 9시 전에는 전날과 당일 알림을 설정한다`() {
        assertEquals(
            EventNotificationWindow.DAY_BEFORE_AND_EVENT_DAY,
            eventNotificationWindow("2026-07-08", eventDayNineAm - 43_200_001),
        )
    }

    @Test
    fun `전날 오후 9시부터 당일 오전 9시 전에는 당일 알림만 설정한다`() {
        assertEquals(
            EventNotificationWindow.EVENT_DAY_ONLY,
            eventNotificationWindow("2026-07-08", eventDayNineAm - 43_200_000),
        )
    }

    @Test
    fun `당일 오전 9시부터 설정을 닫는다`() {
        assertEquals(
            EventNotificationWindow.CLOSED,
            eventNotificationWindow("2026-07-08", eventDayNineAm),
        )
    }
}
