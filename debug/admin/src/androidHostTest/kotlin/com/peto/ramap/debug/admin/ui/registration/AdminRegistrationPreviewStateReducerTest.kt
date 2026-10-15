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
                noticeType = "full_close",
                dailySchedules = listOf(AdminNoticeDay("2026-10-06", true), AdminNoticeDay("2026-10-08", false)),
            )
        val result = AdminRegistrationPreviewStateReducer.reduce(state, preview)
        assertEquals("2026-10-01", result.draft?.startDate)
        assertEquals("2026-10-31", result.draft?.endDate)
        assertEquals(preview.dailySchedules, result.draft?.dailySchedules)
        assertEquals(OperatingNoticeType.TEMPORARY_CLOSURE, result.selectedNoticeType)
    }

    @Test
    fun `임시 휴무를 선택한 미리보기는 조기 마감 시각을 비운다`() {
        val state = AdminRegistrationUiState(isOperatingNotice = true, selectedNoticeType = OperatingNoticeType.TEMPORARY_CLOSURE)
        val preview = AdminDraft(noticeType = "early_close", endTime = "15:00", startDate = "2026-10-15", endDate = "2026-10-15")
        val draft = AdminRegistrationPreviewStateReducer.reduce(state, preview).draft!!
        assertEquals("full_close", draft.noticeType)
        assertNull(draft.endTime)
        assertEquals(true, draft.canRegisterOperatingNotice())
    }

    @Test
    fun `날짜별 하루 휴무는 한달 범위를 버리고 사용자가 선택한 하루를 반영한다`() {
        val state =
            AdminRegistrationUiState(
                isOperatingNotice = true,
                selectedNoticeType = OperatingNoticeType.TEMPORARY_CLOSURE,
                selectedStartDate = "2026-10-15",
                selectedEndDate = "2026-10-15",
            )
        val preview =
            AdminDraft(
                startDate = "2026-10-01",
                endDate = "2026-10-31",
                noticeType = "full_close",
                dailySchedules = listOf(AdminNoticeDay("2026-10-14", true)),
            )

        val draft = AdminRegistrationPreviewStateReducer.reduce(state, preview).draft!!

        assertEquals("2026-10-15", draft.startDate)
        assertEquals("2026-10-15", draft.endDate)
        assertEquals(emptyList(), draft.dailySchedules)
        assertEquals(true, draft.canRegisterOperatingNotice())
    }

    @Test
    fun `날짜 선택이 없으면 날짜별 하루 휴무의 실제 날짜를 사용한다`() {
        val state = AdminRegistrationUiState(isOperatingNotice = true)
        val preview =
            AdminDraft(
                startDate = "2026-10-01",
                endDate = "2026-10-31",
                noticeType = "full_close",
                dailySchedules = listOf(AdminNoticeDay("2026-10-15", true)),
            )

        val draft = AdminRegistrationPreviewStateReducer.reduce(state, preview).draft!!

        assertEquals("2026-10-15", draft.startDate)
        assertEquals("2026-10-15", draft.endDate)
        assertEquals(emptyList(), draft.dailySchedules)
    }

    @Test
    fun `분석 실패로 날짜가 없거나 잘못된 공지는 등록하지 않는다`() {
        for ((start, end) in listOf(null to null, "2026-02-30" to "2026-02-30", "2026-10-16" to "2026-10-15")) {
            val preview = AdminDraft(startDate = start, endDate = end)
            val state = AdminRegistrationUiState(isOperatingNotice = true, selectedNoticeType = OperatingNoticeType.TEMPORARY_CLOSURE)

            val draft = AdminRegistrationPreviewStateReducer.reduce(state, preview).draft!!

            assertEquals(false, draft.canRegisterOperatingNotice())
        }
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
