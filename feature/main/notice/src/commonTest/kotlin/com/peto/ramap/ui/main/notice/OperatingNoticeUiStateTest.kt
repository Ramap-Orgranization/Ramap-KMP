package com.peto.ramap.ui.main.notice

import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class OperatingNoticeUiStateTest {
    @Test
    fun `한 매장의 오늘과 예정 공지는 하나의 진입점에서 모두 보여준다`() {
        val monthly = notice("monthly")
        val closure = notice("closure")
        val future = notice("future")
        val other = notice("other", "other-shop")
        val state = OperatingNoticeUiState(listOf(monthly, closure), listOf(future, other))
        assertEquals(listOf(monthly), state.todayShops)
        assertEquals(listOf(other), state.scheduledShops)
        assertEquals(listOf(monthly, closure, future), state.noticesForShop("shop"))
    }

    private fun notice(
        id: String,
        shopId: String = "shop",
    ) = OperatingNotice(
        id = id,
        shop = ramenShopFixture(id = shopId),
        type = OperatingNoticeType.OPERATING_NOTICE,
        description = "운영 일정",
        startDate = LocalDate.parse("2026-10-01"),
        endDate = LocalDate.parse("2026-10-31"),
        startTime = null,
        endTime = null,
        sourceUrl = null,
    )
}
