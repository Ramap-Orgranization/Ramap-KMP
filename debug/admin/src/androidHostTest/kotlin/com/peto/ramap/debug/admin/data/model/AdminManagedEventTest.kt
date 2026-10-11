package com.peto.ramap.debug.admin.data.model

import com.peto.ramap.debug.admin.FakeAdminRegistrationDataSource.Companion.event
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminManagedEventTest {
    private val today = LocalDate.parse("2026-10-11")

    @Test
    fun `renewal remains editable for one month including month end boundaries`() {
        assertTrue(event("renewal", "2026-09-29", null, "store_renewal").isManageableOn(today))
        assertFalse(event("renewal", "2026-09-11", null, "store_renewal").isManageableOn(today))
        assertEquals("2026-02-27", event("renewal", "2026-01-31", null, "store_renewal").effectiveEndDate)
    }

    @Test
    fun `legacy new menu with no end expires after seven days`() {
        assertFalse(event("new", "2026-10-04", null, "new_menu").isManageableOn(today))
        assertTrue(event("new", "2026-10-05", null, "new_menu").isManageableOn(today))
    }

    @Test
    fun `open ended event stays visible and historical status is not todays status`() {
        val event = event("open", "2026-01-01", null).copy(cancelledDates = listOf("2026-10-10"), soldOutDates = listOf("2026-10-09"))
        assertTrue(event.canChangeStatusOn(today))
        assertFalse(event.isCancelledOn(today))
        assertFalse(event.isSoldOutOn(today))
    }

    @Test
    fun `upcoming event can be edited but today status cannot be changed`() {
        val event = event("future", "2026-10-12", "2026-10-13")
        assertTrue(event.isManageableOn(today))
        assertFalse(event.canChangeStatusOn(today))
    }

    @Test
    fun `schedule correction survives preview decoding and apply encoding`() {
        val preview = Json.decodeFromString<AdminCorrectionPreview>("""{"registration_type":"operating_notice","target_id":"notice","summary":"시간 변경","changes":{"schedule_override":{"open":"11:00","close":"21:00"}}}""")
        assertEquals("11:00", preview.changes.scheduleOverride?.open)
        assertTrue(Json.encodeToString(preview.changes).contains("schedule_override"))
    }
}
