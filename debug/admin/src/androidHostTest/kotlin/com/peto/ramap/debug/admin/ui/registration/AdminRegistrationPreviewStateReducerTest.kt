package com.peto.ramap.debug.admin.ui.registration

import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.data.model.AdminNoticeDay
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationUiState
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminRegistrationPreviewStateReducerTest {
    @Test
    fun `월간 공지는 기존 하루 선택에 잘리지 않고 전체 일정으로 등록한다`() {
        val state =
            AdminRegistrationUiState(
                isOperatingNotice = true,
                selectedEventType = ShopEventType.STORE_RENEWAL,
                selectedNoticeType = OperatingNoticeType.TEMPORARY_CLOSURE,
                selectedStartDate = "2026-10-03",
                selectedEndDate = "2026-10-03",
            )
        val preview =
            AdminDraft(
                startDate = "2026-10-01",
                endDate = "2026-10-31",
                noticeType = "operating_notice",
                dailySchedules = listOf(AdminNoticeDay("2026-10-06", true), AdminNoticeDay("2026-10-08", false)),
            )
        val result = AdminRegistrationPreviewStateReducer.reduce(state, preview)
        assertEquals("2026-10-01", result.draft?.startDate)
        assertEquals("2026-10-31", result.draft?.endDate)
        assertEquals(preview.dailySchedules, result.draft?.dailySchedules)
        assertEquals(OperatingNoticeType.OPERATING_NOTICE, result.selectedNoticeType)
    }

    @Test
    fun `renewal selection changed to limited menu preserves preview end date`() {
        val state = AdminRegistrationUiState(selectedEventType = ShopEventType.STORE_RENEWAL)
        val preview =
            AdminDraft(
                eventType = "limited_menu",
                startDate = "2026-09-01",
                endDate = "2026-09-07",
            )

        val result = AdminRegistrationPreviewStateReducer.reduce(state, preview)

        assertEquals(ShopEventType.LIMITED_MENU, result.selectedEventType)
        assertEquals("2026-09-07", result.draft?.endDate)
    }

    @Test
    fun `limited menu selection changed to renewal clears end date`() {
        val state = AdminRegistrationUiState(selectedEventType = ShopEventType.LIMITED_MENU)
        val preview =
            AdminDraft(
                eventType = "store_renewal",
                startDate = "2026-09-01",
                endDate = "2026-09-07",
            )

        val result = AdminRegistrationPreviewStateReducer.reduce(state, preview)

        assertEquals(ShopEventType.STORE_RENEWAL, result.selectedEventType)
        assertNull(result.draft?.endDate)
    }

    @Test
    fun `invalid preview event type keeps selected event type`() {
        val state = AdminRegistrationUiState(selectedEventType = ShopEventType.POPUP)
        val preview = AdminDraft(eventType = "unknown", endDate = "2026-09-07")

        val result = AdminRegistrationPreviewStateReducer.reduce(state, preview)

        assertEquals(ShopEventType.POPUP, result.selectedEventType)
        assertEquals("2026-09-07", result.draft?.endDate)
    }

    @Test
    fun `user selected dates override preview dates`() {
        val state =
            AdminRegistrationUiState(
                selectedEventType = ShopEventType.LIMITED_MENU,
                selectedStartDate = "2026-09-03",
                selectedEndDate = "2026-09-10",
            )
        val preview =
            AdminDraft(
                eventType = "limited_menu",
                startDate = "2026-09-01",
                endDate = "2026-09-07",
            )

        val result = AdminRegistrationPreviewStateReducer.reduce(state, preview)

        assertEquals("2026-09-03", result.draft?.startDate)
        assertEquals("2026-09-10", result.draft?.endDate)
    }
}
