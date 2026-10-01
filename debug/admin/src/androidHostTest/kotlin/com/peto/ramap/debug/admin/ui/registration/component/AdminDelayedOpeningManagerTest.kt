package com.peto.ramap.debug.admin.ui.registration.component

import com.peto.ramap.debug.admin.data.model.AdminDelayedOpening
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminDelayedOpeningManagerTest {
    @Test
    fun `server korea today separates today notices from future notices`() {
        val today = AdminDelayedOpening(id = "today", noticeDate = "2026-09-12", description = "오늘")
        val scheduled = AdminDelayedOpening(id = "future", noticeDate = "2026-09-13", description = "예정")

        val (todayNotices, scheduledNotices) = partitionByKoreaToday(listOf(scheduled, today), "2026-09-12")

        assertEquals(listOf(today), todayNotices)
        assertEquals(listOf(scheduled), scheduledNotices)
    }
}
